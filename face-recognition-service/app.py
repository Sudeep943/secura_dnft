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
    [0.123, -0.456, ...]   -- 128-dimensional L2-normalised float array
  Error response (JSON, HTTP 4xx/5xx):
    { "error": "reason" }

Design notes for consistent matching
-------------------------------------
The most common cause of low face-match scores (< 0.5) when both images are
taken in the same lighting is **inconsistent preprocessing**:  if the
enrollment image and the attendance image go through different preprocessing
pipelines, the resulting embeddings will be further apart in vector space.

This service addresses that by:
  1. Applying EXIF orientation correction before any processing (crucial for
     mobile-camera images that encode rotation as metadata rather than pixels).
  2. Always normalising brightness/contrast with the same pipeline — both at
     enrollment time and at attendance-check time — so the dlib ResNet model
     receives consistently illuminated, well-contrasted face crops every time.
  3. Always computing embeddings from the normalised image, regardless of
     which detection attempt succeeded.
  4. Returning L2-normalised embeddings so that cosine similarity equals the
     dot product, matching the Java-side comparison logic.

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
  (or set env var FACE_RECOGNITION_SERVICE_URL=http://localhost:5001)

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
from PIL import Image, ImageOps

app = Flask(__name__)
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Configuration (tunable via environment variables)
# ---------------------------------------------------------------------------
_DEFAULT_DETECTION_MODEL = os.getenv("FACE_DETECTION_MODEL", "hog")
_NUM_JITTERS = int(os.getenv("FACE_NUM_JITTERS", "3"))
_UPSAMPLE_TIMES = int(os.getenv("FACE_UPSAMPLE_TIMES", "1"))

# Target mean pixel brightness (0-255) for gamma normalisation.
# Images are corrected toward this target so every embedding is computed
# from a consistently lit face crop.  128 = mid-grey (no-op when image is
# already at this level).
_TARGET_BRIGHTNESS = 128.0

# Minimum face area as a fraction of total image pixels.  Faces smaller than
# this are likely too far from the camera to produce reliable embeddings.
_MIN_FACE_AREA_FRACTION = 0.005  # 0.5 % of total pixels

# Minimum L2 norm below which a vector is considered all-zeros and not normalised.
_MIN_NORM_EPSILON = 1e-9


# ---------------------------------------------------------------------------
# Image helpers
# ---------------------------------------------------------------------------

def _decode_image(image_base64: str) -> np.ndarray:
    """
    Decodes a base64 image string (with or without data-URI prefix) into
    an RGB numpy array suitable for face_recognition.

    Applies EXIF orientation correction so that mobile-camera photos stored
    with rotation metadata are always returned in their upright orientation.
    Without this, a face that is sideways or upside-down in the decoded
    array will often not be detected, or will produce a very different
    embedding than the same face photographed in the correct orientation.
    """
    if "," in image_base64:
        image_base64 = image_base64.split(",", 1)[1]

    image_bytes = base64.b64decode(image_base64.strip())
    pil_image = Image.open(io.BytesIO(image_bytes))
    # Apply EXIF rotation before converting so the pixel array is always upright
    pil_image = ImageOps.exif_transpose(pil_image)
    pil_image = pil_image.convert("RGB")
    return np.array(pil_image)


def _auto_gamma_correction(bgr: np.ndarray,
                            target_mean: float = _TARGET_BRIGHTNESS) -> np.ndarray:
    """
    Applies automatic gamma correction so that the mean pixel brightness
    of the grayscale image approaches *target_mean*.

    The mapping is:  output = (input/255)^(1/gamma) * 255
    where  gamma = log(target/255) / log(mean/255).

    When the image is already at *target_mean*, gamma == 1.0 and the LUT is
    the identity transform (no change).  This makes the function safe to
    call unconditionally without degrading well-exposed images.
    """
    gray = cv2.cvtColor(bgr, cv2.COLOR_BGR2GRAY)
    mean_val = float(np.mean(gray))
    if mean_val < 1:
        return bgr  # fully black – nothing useful to do
    if abs(mean_val - target_mean) < 2:
        return bgr  # already close enough – skip to avoid float rounding noise

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
    improves local contrast without washing out well-lit regions and is safe
    to apply to every image.
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
    return cv2.fastNlMeansDenoisingColored(bgr, None, h=6, hColor=6,
                                           templateWindowSize=7,
                                           searchWindowSize=21)


def _normalize_for_embedding(rgb: np.ndarray) -> np.ndarray:
    """
    Applies a consistent normalisation pipeline to EVERY image, both at
    enrollment time and at attendance-check time.  By passing every image
    through the same deterministic transform, the dlib ResNet model receives
    identically preprocessed crops and therefore produces embeddings that are
    closer together for the same face.

    Pipeline (applied unconditionally):
      1. Auto gamma correction targeting mean brightness _TARGET_BRIGHTNESS
         — a no-op when the image is already well-exposed.
      2. CLAHE on the LAB L-channel for local contrast normalisation.

    Denoising is intentionally excluded from this function because it is slow
    and its effect on embedding consistency is minor compared to brightness
    and contrast normalisation.  It is still applied as a last-resort
    detection fallback via _enhance_image_aggressive().

    Accepts and returns an RGB uint8 numpy array.
    """
    bgr = cv2.cvtColor(rgb, cv2.COLOR_RGB2BGR)
    bgr = _auto_gamma_correction(bgr)
    bgr = _clahe_enhance(bgr)
    return cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB)


def _enhance_image_aggressive(rgb: np.ndarray) -> np.ndarray:
    """
    More aggressive enhancement used only as a last-resort detection fallback
    when the standard normalised image fails face detection.  Adds denoising
    on top of the standard pipeline.

    Accepts and returns an RGB uint8 numpy array.
    """
    bgr = cv2.cvtColor(rgb, cv2.COLOR_RGB2BGR)
    bgr = _denoise(bgr)
    bgr = _auto_gamma_correction(bgr)
    bgr = _clahe_enhance(bgr)
    return cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB)


def _select_largest_face(locations: list) -> list:
    """Returns a single-element list containing the largest detected face."""
    if not locations:
        return locations
    return [max(locations, key=lambda loc: (loc[2] - loc[0]) * (loc[3] - loc[1]))]


def _check_face_size(face_locations: list, image_shape: tuple) -> str | None:
    """
    Returns a warning string if the largest detected face is smaller than
    _MIN_FACE_AREA_FRACTION of the total image area, or None if the size is
    acceptable.
    """
    if not face_locations:
        return None
    h, w = image_shape[:2]
    image_area = h * w
    top, right, bottom, left = face_locations[0]
    face_area = (bottom - top) * (right - left)
    if image_area > 0 and face_area / image_area < _MIN_FACE_AREA_FRACTION:
        return (
            f"Detected face is very small ({face_area} px² vs image {image_area} px²). "
            "Move closer to the camera for a more reliable match."
        )
    return None


# ---------------------------------------------------------------------------
# Multi-attempt face detection
# ---------------------------------------------------------------------------

def _detect_faces(normalised_rgb: np.ndarray, model: str) -> list:
    """
    Tries to detect faces using a cascaded strategy for robustness.

    All attempts operate on either the already-normalised image or a more
    aggressively enhanced version of it.  The caller always uses
    *normalised_rgb* for embedding computation so that preprocessing is
    consistent regardless of which detection attempt succeeded.

    Attempt 1 – normalised image, requested model, standard up-sampling.
    Attempt 2 – aggressively enhanced image, same model, extra up-sampling.
    Attempt 3 – normalised image with CNN model (more accurate, slower).
                Skipped when the primary model is already 'cnn'.
    Attempt 4 – aggressively enhanced image with CNN model.
                Skipped when the primary model is already 'cnn'.

    The aggressively enhanced image is computed lazily (only when attempt 1
    fails) and reused for attempts 2 and 4.

    Returns a list of face_location tuples (may be empty).
    """
    upsample = _UPSAMPLE_TIMES

    # Attempt 1: normalised image
    locs = face_recognition.face_locations(normalised_rgb,
                                            number_of_times_to_upsample=upsample,
                                            model=model)
    if locs:
        logger.debug("Faces found on attempt 1 (normalised, %s).", model)
        return locs

    # Compute aggressively enhanced image once for attempts 2 and 4
    logger.info("No faces on attempt 1; applying aggressive enhancement for further attempts.")
    aggressively_enhanced = _enhance_image_aggressive(normalised_rgb)

    # Attempt 2: aggressively enhanced image with extra up-sampling
    locs = face_recognition.face_locations(aggressively_enhanced,
                                            number_of_times_to_upsample=upsample + 1,
                                            model=model)
    if locs:
        logger.debug("Faces found on attempt 2 (aggressively enhanced, %s).", model)
        return locs

    # Attempts 3 & 4: CNN model (skip if already using CNN)
    if model != "cnn":
        # Attempt 3: CNN model on normalised image
        logger.info("No faces on attempt 2; trying CNN model on normalised image.")
        locs = face_recognition.face_locations(normalised_rgb,
                                                number_of_times_to_upsample=upsample,
                                                model="cnn")
        if locs:
            logger.debug("Faces found on attempt 3 (normalised, cnn).")
            return locs

        # Attempt 4: CNN model on aggressively enhanced image
        logger.info("No faces on attempt 3; trying CNN model on aggressively enhanced image.")
        locs = face_recognition.face_locations(aggressively_enhanced,
                                                number_of_times_to_upsample=upsample + 1,
                                                model="cnn")
        if locs:
            logger.debug("Faces found on attempt 4 (aggressively enhanced, cnn).")
            return locs

    return []


# ---------------------------------------------------------------------------
# Flask routes
# ---------------------------------------------------------------------------

@app.route("/extract-embedding", methods=["POST"])
def extract_embedding():
    """
    Extracts a 128-dimensional face embedding from the provided base64 image.

    Key design properties for reliable matching:

    Consistency — every image (enrollment and attendance) goes through the
      same _normalize_for_embedding() pipeline before the embedding is
      computed.  This is the primary fix for low match scores: if enrollment
      and attendance images are preprocessed differently, the resulting
      embeddings will be further apart even for the same face.

    EXIF correction — _decode_image() applies ImageOps.exif_transpose() so
      that mobile-camera photos are always upright before processing.

    L2 normalisation — the returned embedding is unit-length so that cosine
      similarity equals the dot product on the Java side.

    Robust detection — a multi-attempt pipeline (normalised → aggressively
      enhanced → CNN) maximises detection reliability in difficult conditions,
      without affecting embedding consistency (embeddings are always computed
      from the normalised image, not the detection image).
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

    # Always normalise before detection and embedding — this is the key to
    # producing consistent embeddings across enrollment and attendance sessions.
    normalised = _normalize_for_embedding(image_array)

    face_locations = _detect_faces(normalised, _DEFAULT_DETECTION_MODEL)

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

    size_warning = _check_face_size(face_locations, normalised.shape)
    if size_warning:
        logger.warning(size_warning)

    # Compute embedding from the NORMALISED image — the same pipeline that
    # will be used at attendance time — so enrollment and attendance embeddings
    # are always comparable.
    encodings = face_recognition.face_encodings(
        normalised,
        known_face_locations=face_locations,
        num_jitters=_NUM_JITTERS,
    )

    if not encodings:
        return jsonify({"error": "Could not compute face embedding. Please use a clearer, front-facing photo."}), 422

    # L2-normalise so the embedding is a unit vector.  Cosine similarity then
    # equals the dot product, matching the Java-side comparison.
    embedding_np = encodings[0]
    norm = float(np.linalg.norm(embedding_np))
    if norm > _MIN_NORM_EPSILON:
        embedding_np = embedding_np / norm

    result = embedding_np.tolist()
    if size_warning:
        # Embed the warning in a structured response so the caller can surface it
        return jsonify({"embedding": result, "warning": size_warning}), 200
    return jsonify(result), 200


@app.route("/health", methods=["GET"])
def health():
    return jsonify({
        "status": "ok",
        "model": _DEFAULT_DETECTION_MODEL,
        "num_jitters": _NUM_JITTERS,
        "upsample_times": _UPSAMPLE_TIMES,
    }), 200


if __name__ == "__main__":
    port = int(os.getenv("PORT", "5001"))
    debug = os.getenv("FLASK_DEBUG", "false").lower() == "true"
    logger.info(
        "Starting Secura Face Recognition Service on port %d "
        "(model=%s, num_jitters=%d, upsample=%d)",
        port, _DEFAULT_DETECTION_MODEL, _NUM_JITTERS, _UPSAMPLE_TIMES,
    )
    app.run(host="0.0.0.0", port=port, debug=debug)
