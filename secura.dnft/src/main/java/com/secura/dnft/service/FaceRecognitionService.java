package com.secura.dnft.service;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * FaceRecognitionService provides face embedding extraction and comparison.
 *
 * When {@code face.recognition.service.url} is configured, embedding extraction
 * delegates to that external service (e.g. a Python microservice using
 * InsightFace/DeepFace, or AWS Rekognition). The service must expose:
 *   POST {url}/extract-embedding
 *   Body:     {"image_base64": "<base64>"}
 *   Response: float[] JSON array  (e.g. [0.12, -0.34, ...])
 *
 * When the property is not set, a deterministic stub is used: SHA-256 of the
 * image bytes is expanded via counter-mode hashing to fill a 512-D unit vector.
 * The stub is collision-resistant (uses all 256 hash bits) and allows end-to-end
 * testing when the SAME image bytes are submitted at enrolment and attendance.
 * It will NOT match different photos of the same face — configure the external
 * service for real face recognition.
 */
@Service
public class FaceRecognitionService {

    private static final int EMBEDDING_DIM = 512;
    private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024; // 10 MB

    @Value("${face.recognition.service.url:}")
    private String faceServiceUrl;

    @Autowired
    private RestTemplate restTemplate;

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

        if (faceServiceUrl != null && !faceServiceUrl.isBlank()) {
            return extractEmbeddingFromService(raw);
        }
        return buildEmbedding(imageBytes);
    }

    /**
     * Calls the configured external face recognition service to obtain a real
     * face embedding for the given image.
     *
     * @param imageBase64 raw base64 string (data-URI prefix already stripped)
     * @return 512-d float embedding returned by the service
     */
    private float[] extractEmbeddingFromService(String imageBase64) {
        try {
            Map<String, String> request = Map.of("image_base64", imageBase64);
            float[] embedding = restTemplate.postForObject(
                    faceServiceUrl + "/extract-embedding", request, float[].class);
            if (embedding == null || embedding.length == 0) {
                throw new IllegalArgumentException("External face service returned an empty embedding");
            }
            if (embedding.length != EMBEDDING_DIM) {
                throw new IllegalArgumentException(
                        "External face service returned an embedding of unexpected dimension: "
                        + embedding.length + " (expected " + EMBEDDING_DIM + ")");
            }
            return embedding;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to contact face recognition service: " + e.getMessage(), e);
        }
    }

    /**
     * Derives a deterministic 512-d unit vector from the raw image bytes using
     * SHA-256 counter-mode expansion.
     *
     * <p>The full 32-byte SHA-256 digest of the image is used as a seed. The
     * seed is then expanded to the required number of bytes by repeatedly
     * computing SHA-256(seed || counter) for counter = 0, 1, 2, … This uses
     * all 256 bits of hash entropy and avoids the 48-bit seed limitation of
     * {@link java.util.Random}.
     */
    private float[] buildEmbedding(byte[] imageBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] seed = digest.digest(imageBytes); // full 32-byte seed

            // Expand seed to EMBEDDING_DIM * 4 bytes via SHA-256 counter mode
            int bytesNeeded = EMBEDDING_DIM * 4;
            byte[] randomBytes = new byte[bytesNeeded];
            int offset = 0;
            int counter = 0;
            while (offset < bytesNeeded) {
                digest.reset();
                digest.update(seed);
                digest.update((byte) (counter >>> 24));
                digest.update((byte) (counter >>> 16));
                digest.update((byte) (counter >>> 8));
                digest.update((byte) counter);
                byte[] block = digest.digest(); // 32 bytes per round
                int toCopy = Math.min(block.length, bytesNeeded - offset);
                System.arraycopy(block, 0, randomBytes, offset, toCopy);
                offset += toCopy;
                counter++;
            }

            // Interpret each 4-byte group as a signed 32-bit integer, cast to float
            float[] embedding = new float[EMBEDDING_DIM];
            double sumSq = 0.0;
            for (int i = 0; i < EMBEDDING_DIM; i++) {
                int raw = ((randomBytes[i * 4] & 0xFF) << 24)
                        | ((randomBytes[i * 4 + 1] & 0xFF) << 16)
                        | ((randomBytes[i * 4 + 2] & 0xFF) << 8)
                        | (randomBytes[i * 4 + 3] & 0xFF);
                embedding[i] = (float) raw;
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

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Parses a stored JSON array string back to a float[].
     * Expected format: [0.123, -0.456, ...]
     */
    public float[] parseEmbedding(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("Embedding JSON must not be empty");
        }
        try {
            float[] result = OBJECT_MAPPER.readValue(json, float[].class);
            if (result == null || result.length == 0) {
                throw new IllegalArgumentException("Embedding array is empty");
            }
            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse embedding JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Serialises a float[] to a compact JSON array string using Jackson.
     */
    public String embeddingToJson(float[] embedding) {
        try {
            return OBJECT_MAPPER.writeValueAsString(embedding);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialise embedding", e);
        }
    }
}
