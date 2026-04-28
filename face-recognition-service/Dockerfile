# Build image: docker build -t secura-face-svc .
# Run:         docker run -p 5001:5001 secura-face-svc

FROM python:3.11-slim

# dlib (required by face_recognition) needs build tools and cmake
RUN apt-get update && apt-get install -y --no-install-recommends \
        build-essential \
        cmake \
        libopenblas-dev \
        liblapack-dev \
        libx11-dev \
        libgtk-3-dev \
        libboost-python-dev \
        && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app.py .

EXPOSE 5001

# Pre-download dlib face landmark/encoding models on build (avoids cold-start delay)
RUN python -c "import face_recognition; import numpy as np; \
    import face_recognition_models; print('Models ready:', face_recognition_models.pose_predictor_model_location())"

ENV PORT=5001
ENV FACE_DETECTION_MODEL=hog

CMD ["python", "app.py"]
