"""
Secura Face Recognition Microservice
=====================================
Provides a REST endpoint to extract 128-dimensional face embeddings from
base64-encoded images. The embeddings are derived from 68 facial landmarks
(eyes, nose, mouth, jawline, eyebrows) using dlib's ResNet-based model via
the face_recognition library.

Endpoint
--------
POST /extract-embedding
  Request body (JSON):
    { "image_base64": "<base64-encoded JPEG or PNG>" }
  Response (JSON, HTTP 200):
    [0.123, -0.456, ...]   -- 128-dimensional float array
  Error response (JSON, HTTP 4xx/5xx):
    { "error": "reason" }

Usage
-----
  pip install -r requirements.txt
  python app.py

Docker
------
  docker build -t secura-face-svc .
  docker run -p 5001:5001 secura-face-svc

Configure the Spring Boot service by setting:
  face.recognition.service.url=http://localhost:5001

Environment variables
---------------------
  FACE_DETECTION_MODEL  hog (default) | cnn  – primary detection model.
                        CNN is more accurate but slower (no GPU required).
  FACE_NUM_JITTERS      Number of times to re-sample the face when calculating
                        the embedding (default: 3). Higher = more accurate but
                        slower. Use 1 for fastest results.
  FACE_UPSAMPLE_TIMES   Number of times to upsample the image looking for faces
                        (default: 1). 2 finds smaller/more-distant faces.
  PORT                  HTTP port (default: 5001)
  FLASK_DEBUG           true | false (default: false)
"""

import base64
import io
import logging
import os

import cv2
import face_recognition
import numpy as np
from flask import Flask, jsonify, request
from PIL import Image

app = Flask(__name__)
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Configuration (tunable via environment variables)
# ---------------------------------------------------------------------------
_DEFAULT_DETECTION_MODEL = os.getenv("FACE_DETECTION_MODEL", "hog")
_NUM_JITTERS = int(os.getenv("FACE_NUM_JITTERS", "3"))
_UPSAMPLE_TIMES = int(os.getenv("FACE_UPSAMPLE_TIMES", "1"))

# Brightness threshold below which the image is considered low-light.
# Mean pixel value (0-255) of the grayscale image; 85 ≈ one-third of full scale.
_LOW_LIGHT_THRESHOLD = 85


# ---------------------------------------------------------------------------
# Image helpers
# ---------------------------------------------------------------------------

def _decode_image(image_base64: str) -> np.ndarray:
    """
    Decodes a base64 image string (with or without data-URI prefix) into
    an RGB numpy array suitable for face_recognition.
    """
    if "," in image_base64:
        image_base64 = image_base64.split(",", 1)[1]

    image_bytes = base64.b64decode(image_base64.strip())
    pil_image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
    return np.array(pil_image)


def _auto_gamma_correction(bgr: np.ndarray, target_mean: float = 128.0) -> np.ndarray:
    """
    Applies automatic gamma correction so that the mean pixel brightness
    of the grayscale image approaches *target_mean*.  This brightens
    underexposed (low-light) images and slightly dims overexposed ones.
    """
    gray = cv2.cvtColor(bgr, cv2.COLOR_BGR2GRAY)
    mean_val = float(np.mean(gray))
    if mean_val < 1:
        return bgr  # fully black – nothing useful to do

    # gamma = log(target) / log(mean)  →  maps mean → target after power-law
    gamma = np.log(target_mean / 255.0) / np.log(mean_val / 255.0)
    gamma = float(np.clip(gamma, 0.2, 5.0))

    inv_gamma = 1.0 / gamma
    lut = np.array(
        [((i / 255.0) ** inv_gamma) * 255 for i in range(256)],
        dtype=np.uint8,
    )
    return cv2.LUT(bgr, lut)


def _clahe_enhance(bgr: np.ndarray) -> np.ndarray:
    """
    Applies CLAHE (Contrast Limited Adaptive Histogram Equalisation) to the
    L-channel of the LAB colour space, then converts back to BGR.  This
    improves local contrast without washing out well-lit regions.
    """
    lab = cv2.cvtColor(bgr, cv2.COLOR_BGR2LAB)
    l_ch, a_ch, b_ch = cv2.split(lab)
    clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
    l_ch = clahe.apply(l_ch)
    lab = cv2.merge((l_ch, a_ch, b_ch))
    return cv2.cvtColor(lab, cv2.COLOR_LAB2BGR)


def _denoise(bgr: np.ndarray) -> np.ndarray:
    """
    Removes sensor noise / bad pixels using OpenCV's non-local means
    denoising.  Parameters are tuned for a balance between noise suppression
    and preserving facial detail.
    """
    # h=6 is conservative – reduces salt/pepper artefacts without blurring features
    return cv2.fastNlMeansDenoisingColored(bgr, None, h=6, hColor=6,
                                           templateWindowSize=7,
                                           searchWindowSize=21)


def _enhance_image(rgb: np.ndarray) -> np.ndarray:
    """
    Full preprocessing pipeline designed for low-light or noisy images:
      1. Denoise (bad-pixel / sensor noise removal)
      2. Auto gamma correction (brightens dark images)
      3. CLAHE on L channel (local contrast enhancement)

    Accepts and returns an RGB uint8 numpy array.
    """
    bgr = cv2.cvtColor(rgb, cv2.COLOR_RGB2BGR)

    gray = cv2.cvtColor(bgr, cv2.COLOR_BGR2GRAY)
    is_low_light = float(np.mean(gray)) < _LOW_LIGHT_THRESHOLD

    # Always denoise to handle bad pixels
    bgr = _denoise(bgr)

    if is_low_light:
        bgr = _auto_gamma_correction(bgr)
        bgr = _clahe_enhance(bgr)

    return cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB)


def _select_largest_face(locations: list) -> list:
    """Returns a single-element list containing the largest detected face."""
    if not locations:
        return locations
    return [max(locations, key=lambda loc: (loc[2] - loc[0]) * (loc[3] - loc[1]))]


# ---------------------------------------------------------------------------
# Multi-attempt face detection
# ---------------------------------------------------------------------------

def _detect_faces(image_rgb: np.ndarray, model: str) -> tuple[list, np.ndarray]:
    """
    Tries to detect faces using a cascaded strategy for robustness:

    Attempt 1 – original image, requested model, standard up-sampling.
    Attempt 2 – enhanced image (denoise + gamma + CLAHE), same model,
                extra up-sampling (finds smaller / darker faces).
    Attempt 3 – original image with CNN model (more accurate, slower).
                Skipped when the primary model is already 'cnn'.
    Attempt 4 – enhanced image with CNN model.
                Skipped when the primary model is already 'cnn'.

    The enhanced image is computed lazily (only when attempt 1 fails) and
    reused for both attempt 2 and attempt 4.

    Returns (face_locations, image_used_for_detection).
    """
    upsample = _UPSAMPLE_TIMES

    # Attempt 1: original image
    locs = face_recognition.face_locations(image_rgb, number_of_times_to_upsample=upsample, model=model)
    if locs:
        logger.debug("Faces found on attempt 1 (original, %s).", model)
        return locs, image_rgb

    # Compute enhanced image once for attempts 2 and 4
    logger.info("No faces on attempt 1; enhancing image for further attempts.")
    enhanced = _enhance_image(image_rgb)

    # Attempt 2: enhanced image with extra up-sampling
    locs = face_recognition.face_locations(enhanced, number_of_times_to_upsample=upsample + 1, model=model)
    if locs:
        logger.debug("Faces found on attempt 2 (enhanced, %s).", model)
        return locs, enhanced

    # Attempts 3 & 4: CNN model (skip if already using CNN)
    if model != "cnn":
        # Attempt 3: CNN model on original image
        logger.info("No faces on attempt 2; trying CNN model on original image.")
        locs = face_recognition.face_locations(image_rgb, number_of_times_to_upsample=upsample, model="cnn")
        if locs:
            logger.debug("Faces found on attempt 3 (original, cnn).")
            return locs, image_rgb

        # Attempt 4: CNN model on enhanced image (reuse already-computed enhanced)
        logger.info("No faces on attempt 3; trying CNN model on enhanced image.")
        locs = face_recognition.face_locations(enhanced, number_of_times_to_upsample=upsample + 1, model="cnn")
        if locs:
            logger.debug("Faces found on attempt 4 (enhanced, cnn).")
            return locs, enhanced

    return [], image_rgb


# ---------------------------------------------------------------------------
# Flask routes
# ---------------------------------------------------------------------------

@app.route("/extract-embedding", methods=["POST"])
def extract_embedding():
    """
    Extracts a 128-dimensional face embedding from the provided base64 image.

    Preprocessing improvements for low-light / bad-pixel robustness:
      • Automatic gamma correction to brighten underexposed images.
      • CLAHE (Contrast Limited Adaptive Histogram Equalisation) on the
        LAB L-channel for local contrast enhancement.
      • Non-local means denoising to remove sensor noise / bad pixels.

    Detection improvements:
      • Multi-attempt detection pipeline (original → enhanced → CNN).
      • Configurable up-sampling to find small or distant faces.

    Embedding improvements:
      • Configurable num_jitters (default 3) for higher embedding accuracy
        at the cost of modest extra compute time.
    """
    body = request.get_json(force=True, silent=True)
    if not body or "image_base64" not in body:
        return jsonify({"error": "Missing 'image_base64' field in request body"}), 400

    image_base64 = body["image_base64"]
    if not image_base64 or not image_base64.strip():
        return jsonify({"error": "image_base64 must not be empty"}), 400

    try:
        image_array = _decode_image(image_base64)
    except Exception as e:
        logger.warning("Failed to decode image: %s", e)
        return jsonify({"error": "Invalid image data. Ensure the image is a valid base64-encoded JPEG or PNG."}), 400

    face_locations, detection_image = _detect_faces(image_array, _DEFAULT_DETECTION_MODEL)

    if not face_locations:
        return jsonify({
            "error": (
                "No face detected in the image. "
                "Tips: ensure there is sufficient lighting, face the camera directly, "
                "avoid heavy shadows or obstructions, and use a photo where the face "
                "occupies a reasonable portion of the frame."
            )
        }), 422

    if len(face_locations) > 1:
        logger.info("Multiple faces detected (%d); using the largest.", len(face_locations))
        face_locations = _select_largest_face(face_locations)

    # Higher num_jitters = more stable / accurate embeddings (trades speed for accuracy)
    encodings = face_recognition.face_encodings(
        detection_image,
        known_face_locations=face_locations,
        num_jitters=_NUM_JITTERS,
    )

    if not encodings:
        return jsonify({"error": "Could not compute face embedding. Please use a clearer, front-facing photo."}), 422

    embedding = encodings[0].tolist()
    return jsonify(embedding), 200


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok"}), 200


if __name__ == "__main__":
    port = int(os.getenv("PORT", "5001"))
    debug = os.getenv("FLASK_DEBUG", "false").lower() == "true"
    logger.info(
        "Starting Secura Face Recognition Service on port %d "
        "(model=%s, num_jitters=%d, upsample=%d)",
        port, _DEFAULT_DETECTION_MODEL, _NUM_JITTERS, _UPSAMPLE_TIMES,
    )
    app.run(host="0.0.0.0", port=port, debug=debug)
