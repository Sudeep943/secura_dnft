package com.secura.dnft.service;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * FaceRecognitionService provides face embedding extraction and comparison.
 *
 * When {@code face.recognition.service.url} is configured, embedding extraction
 * delegates to that external service (e.g. the bundled Python microservice in
 * {@code face-recognition-service/} which uses dlib's ResNet model via the
 * face_recognition library). The service must expose:
 *   POST {url}/extract-embedding
 *   Body:     {"image_base64": "<base64>"}
 *   Response: float[] JSON array  (e.g. [0.12, -0.34, ...], any length >= 32)
 *             OR {"embedding": [...], "warning": "..."} when a quality warning exists
 *
 * The face_recognition library returns 128-dimensional embeddings derived from
 * 68 facial landmarks (eyes, nose, mouth, eyebrows, jawline). These are robust
 * to moderate changes in lighting, angle, and expression — the same face in
 * different photos will produce similar embeddings (cosine similarity > ~0.5).
 *
 * The Python microservice always applies the same brightness/contrast
 * normalisation pipeline to every image (both at enrollment and at attendance
 * time), which is the key to producing consistent embeddings.  All embeddings
 * returned by the service are L2-normalised unit vectors.  The Java side also
 * L2-normalises them defensively so that cosine similarity == dot product holds
 * regardless of the back-end model.
 *
 * When the property is not set, a deterministic stub is used: SHA-256 of the
 * image bytes is expanded via counter-mode hashing to fill a 512-D unit vector.
 * The stub is for integration testing only — it matches IDENTICAL image bytes
 * but CANNOT match different photos of the same face. Configure the external
 * service for real face recognition.
 *
 * IMPORTANT: After switching from the stub to the external service (or vice
 * versa), all employees must be re-enrolled because the embeddings are
 * incompatible between the two modes.  Use POST
 * /api/v1/admin/employees/{employeeCode}/re-enroll to replace stored embeddings
 * without deleting the employee's attendance history.
 */
@Service
public class FaceRecognitionService {

    private static final Logger logger = LoggerFactory.getLogger(FaceRecognitionService.class);

    /** Dimension used by the built-in SHA-256 stub (testing only). */
    private static final int STUB_EMBEDDING_DIM = 512;

    /** Minimum embedding dimension accepted from the external service. */
    private static final int MIN_SERVICE_EMBEDDING_DIM = 32;

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
     * <p>The service returns either:
     * <ul>
     *   <li>A plain JSON float array: {@code [0.12, -0.34, ...]}</li>
     *   <li>A structured object when a quality warning exists:
     *       {@code {"embedding": [...], "warning": "..."}}</li>
     * </ul>
     *
     * <p>The service may return embeddings of any dimension ≥
     * {@value #MIN_SERVICE_EMBEDDING_DIM}. The returned embedding is always
     * L2-normalised to a unit vector so that cosine similarity == dot product.
     *
     * @param imageBase64 raw base64 string (data-URI prefix already stripped)
     * @return L2-normalised float[] embedding (length >= MIN_SERVICE_EMBEDDING_DIM)
     */
    private float[] extractEmbeddingFromService(String imageBase64) {
        try {
            Map<String, String> requestBody = Map.of("image_base64", imageBase64);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody);

            // Use String response so we can handle both array and object responses
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    faceServiceUrl + "/extract-embedding",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<String>() {});

            String responseBody = responseEntity.getBody();
            if (responseBody == null || responseBody.isBlank()) {
                throw new IllegalArgumentException("External face service returned an empty response");
            }

            float[] embedding = parseServiceResponse(responseBody);

            if (embedding.length < MIN_SERVICE_EMBEDDING_DIM) {
                throw new IllegalArgumentException(
                        "External face service returned an embedding that is too short: "
                        + embedding.length + " (minimum " + MIN_SERVICE_EMBEDDING_DIM + ")");
            }

            // L2-normalise so that cosine similarity == dot product, regardless of
            // whether the back-end model already normalised the vector.
            return l2Normalize(embedding);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to contact face recognition service: " + e.getMessage(), e);
        }
    }

    /**
     * Parses the raw JSON body returned by the external face service.
     * Accepts both a plain float array and the structured
     * {@code {"embedding": [...], "warning": "..."}} format.
     */
    private float[] parseServiceResponse(String responseBody) {
        try {
            JsonNode node = OBJECT_MAPPER.readTree(responseBody);
            if (node.isArray()) {
                float[] result = OBJECT_MAPPER.treeToValue(node, float[].class);
                if (result == null || result.length == 0) {
                    throw new IllegalArgumentException("External face service returned an empty embedding");
                }
                return result;
            }
            if (node.isObject() && node.has("embedding")) {
                JsonNode embNode = node.get("embedding");
                float[] result = OBJECT_MAPPER.treeToValue(embNode, float[].class);
                if (result == null || result.length == 0) {
                    throw new IllegalArgumentException("External face service returned an empty embedding");
                }
                if (node.has("warning")) {
                    logger.warn("Face service quality warning: {}", node.get("warning").asText());
                }
                return result;
            }
            if (node.isObject() && node.has("error")) {
                throw new IllegalArgumentException("Face service error: " + node.get("error").asText());
            }
            throw new IllegalArgumentException("Unexpected response format from face service");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse face service response: " + e.getMessage(), e);
        }
    }

    /**
     * Returns a new L2-normalised copy of the given vector.
     * If the vector is all zeros the original array is returned unchanged.
     */
    private float[] l2Normalize(float[] v) {
        double sumSq = 0.0;
        for (float x : v) {
            sumSq += (double) x * x;
        }
        float norm = (float) Math.sqrt(sumSq);
        if (norm < 1e-9f) {
            return v;
        }
        float[] normalised = new float[v.length];
        for (int i = 0; i < v.length; i++) {
            normalised[i] = v[i] / norm;
        }
        return normalised;
    }

    /**
     * Derives a deterministic {@value #STUB_EMBEDDING_DIM}-d unit vector from
     * the raw image bytes using SHA-256 counter-mode expansion.
     *
     * <p><strong>This stub is for integration testing only.</strong> It matches
     * when the EXACT same image bytes are submitted at enrolment and attendance,
     * but produces a completely different embedding for any other image (even a
     * different photo of the same person). Configure {@code face.recognition.service.url}
     * to use the real face recognition microservice.
     *
     * <p>The full 32-byte SHA-256 digest of the image is used as a seed. The
     * seed is then expanded to the required number of bytes by repeatedly
     * computing SHA-256(seed || counter) for counter = 0, 1, 2, ... This uses
     * all 256 bits of hash entropy and avoids the 48-bit seed limitation of
     * {@link java.util.Random}.
     */
    private float[] buildEmbedding(byte[] imageBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] seed = digest.digest(imageBytes); // full 32-byte seed

            // Expand seed to STUB_EMBEDDING_DIM * 4 bytes via SHA-256 counter mode
            int bytesNeeded = STUB_EMBEDDING_DIM * 4;
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
            float[] embedding = new float[STUB_EMBEDDING_DIM];
            double sumSq = 0.0;
            for (int i = 0; i < STUB_EMBEDDING_DIM; i++) {
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
                for (int i = 0; i < STUB_EMBEDDING_DIM; i++) {
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
