package com.secura.dnft.service;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.Random;

import org.springframework.stereotype.Service;

/**
 * FaceRecognitionService provides face embedding extraction and comparison.
 *
 * NOTE: The current implementation is a deterministic stub that derives a
 * 512-dimensional unit-norm vector from the SHA-256 hash of the image bytes.
 * This means the same image will always produce the same embedding, which
 * allows the attendance matching logic to function end-to-end.
 *
 * For production, replace extractEmbedding() with a call to a real face
 * recognition backend (e.g. a Python microservice using InsightFace/DeepFace,
 * or a cloud service such as AWS Rekognition).
 */
@Service
public class FaceRecognitionService {

    private static final int EMBEDDING_DIM = 512;
    private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024; // 10 MB

    /**
     * Decodes a base64 image string, validates it, and returns a 512-d float
     * embedding vector.
     *
     * @param imageBase64 base64-encoded image (JPEG or PNG)
     * @return unit-normalised 512-d embedding, never null
     * @throws IllegalArgumentException if the input is invalid
     */
    public float[] extractEmbedding(String imageBase64) {
        if (imageBase64 == null || imageBase64.isBlank()) {
            throw new IllegalArgumentException("Image data must not be empty");
        }

        // Strip optional data-URI prefix (data:image/jpeg;base64,...)
        String raw = imageBase64;
        int commaIdx = raw.indexOf(',');
        if (commaIdx >= 0) {
            raw = raw.substring(commaIdx + 1);
        }

        byte[] imageBytes;
        try {
            imageBytes = Base64.getDecoder().decode(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid base64 image data", e);
        }

        if (imageBytes.length == 0) {
            throw new IllegalArgumentException("Decoded image is empty");
        }
        if (imageBytes.length > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Image exceeds maximum allowed size of 10 MB");
        }

        return buildEmbedding(imageBytes);
    }

    /**
     * Derives a deterministic 512-d unit vector from the raw image bytes using
     * SHA-256 hashing and a seeded PRNG.
     */
    private float[] buildEmbedding(byte[] imageBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(imageBytes);

            // Convert first 8 bytes of hash to a long seed
            long seed = 0;
            for (int i = 0; i < 8; i++) {
                seed = (seed << 8) | (hash[i] & 0xFF);
            }

            Random rng = new Random(seed);
            float[] embedding = new float[EMBEDDING_DIM];
            double sumSq = 0.0;
            for (int i = 0; i < EMBEDDING_DIM; i++) {
                embedding[i] = (float) rng.nextGaussian();
                sumSq += (double) embedding[i] * embedding[i];
            }

            // L2 normalise
            float norm = (float) Math.sqrt(sumSq);
            if (norm > 0) {
                for (int i = 0; i < EMBEDDING_DIM; i++) {
                    embedding[i] /= norm;
                }
            }
            return embedding;

        } catch (Exception e) {
            throw new RuntimeException("Failed to build face embedding", e);
        }
    }

    /**
     * Cosine similarity between two unit-normalised vectors.
     * Returns a value in [-1, 1]; higher is more similar.
     */
    public double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Embedding dimension mismatch");
        }
        double dot = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
        }
        // Both vectors are already unit-normalised, so cosine similarity == dot product
        return dot;
    }

    /**
     * Parses a stored JSON array string back to a float[].
     * Expected format: [0.123, -0.456, ...]
     */
    public float[] parseEmbedding(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("Embedding JSON must not be empty");
        }
        String trimmed = json.trim();
        if (trimmed.startsWith("[")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("]")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        String[] parts = trimmed.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }

    /**
     * Serialises a float[] to a compact JSON array string.
     */
    public String embeddingToJson(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(embedding[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
