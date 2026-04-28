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
"""

import base64
import io
import logging
import os

import face_recognition
import numpy as np
from flask import Flask, jsonify, request
from PIL import Image

app = Flask(__name__)
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def _decode_image(image_base64: str) -> np.ndarray:
    """
    Decodes a base64 image string (with or without data-URI prefix) into
    an RGB numpy array suitable for face_recognition.
    """
    # Strip optional data-URI prefix (data:image/jpeg;base64,...)
    if "," in image_base64:
        image_base64 = image_base64.split(",", 1)[1]

    image_bytes = base64.b64decode(image_base64.strip())
    pil_image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
    return np.array(pil_image)


@app.route("/extract-embedding", methods=["POST"])
def extract_embedding():
    """
    Extracts a 128-dimensional face embedding from the provided base64 image.

    Uses face_recognition (dlib ResNet model) which builds a descriptor from
    68 facial landmarks including:
      - Eyes (outer/inner corners, eyelids)
      - Nose bridge and tip
      - Mouth corners and lip edges
      - Eyebrows
      - Jawline contour

    This landmark-based approach is robust to moderate changes in lighting,
    angle, and expression — the same person's face in different photos will
    produce similar embeddings (cosine similarity typically > 0.8).
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
        return jsonify({"error": f"Invalid image data: {e}"}), 400

    # Detect face locations first (HOG model is fast; use CNN for better accuracy)
    detection_model = os.getenv("FACE_DETECTION_MODEL", "hog")
    face_locations = face_recognition.face_locations(image_array, model=detection_model)

    if not face_locations:
        return jsonify({"error": "No face detected in the image. Ensure good lighting, face the camera directly, and avoid obstructions."}), 422

    if len(face_locations) > 1:
        logger.info("Multiple faces detected (%d); using the largest face.", len(face_locations))
        # Pick the face with the largest bounding box (most prominent in frame)
        face_locations = [max(
            face_locations,
            key=lambda loc: (loc[2] - loc[0]) * (loc[1] - loc[3])
        )]

    # Extract the 128-d face descriptor from the detected face
    encodings = face_recognition.face_encodings(image_array, known_face_locations=face_locations, num_jitters=1)

    if not encodings:
        return jsonify({"error": "Could not compute face embedding. Please use a clearer, front-facing photo."}), 422

    embedding = encodings[0].tolist()  # numpy float64[] -> Python list of floats
    return jsonify(embedding), 200


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok"}), 200


if __name__ == "__main__":
    port = int(os.getenv("PORT", "5001"))
    debug = os.getenv("FLASK_DEBUG", "false").lower() == "true"
    logger.info("Starting Secura Face Recognition Service on port %d", port)
    app.run(host="0.0.0.0", port=port, debug=debug)
