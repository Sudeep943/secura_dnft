package com.secura.dnft.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class FaceRecognitionServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private FaceRecognitionService faceRecognitionService;

    private static final String SAMPLE_IMAGE_BASE64 =
            Base64.getEncoder().encodeToString("fake-image-bytes-for-testing".getBytes());

    @BeforeEach
    void setUp() {
        // faceServiceUrl defaults to "" (empty) via @Value default — stub mode
    }

    // -------------------------------------------------------------------------
    // Stub mode: same image bytes always produce the same embedding
    // -------------------------------------------------------------------------

    @Test
    void extractEmbedding_shouldBeDeterministicForSameInput() {
        float[] first = faceRecognitionService.extractEmbedding(SAMPLE_IMAGE_BASE64);
        float[] second = faceRecognitionService.extractEmbedding(SAMPLE_IMAGE_BASE64);

        assertArrayEquals(first, second,
                "Same image bytes must always produce the same embedding");
    }

    @Test
    void extractEmbedding_shouldProduceDifferentEmbeddingsForDifferentImages() {
        String otherImage = Base64.getEncoder().encodeToString("different-image-bytes".getBytes());

        float[] first = faceRecognitionService.extractEmbedding(SAMPLE_IMAGE_BASE64);
        float[] second = faceRecognitionService.extractEmbedding(otherImage);

        boolean differs = false;
        for (int i = 0; i < first.length; i++) {
            if (first[i] != second[i]) {
                differs = true;
                break;
            }
        }
        assertTrue(differs, "Different image bytes must produce different embeddings");
    }

    @Test
    void extractEmbedding_shouldReturnUnitNormVector() {
        float[] embedding = faceRecognitionService.extractEmbedding(SAMPLE_IMAGE_BASE64);

        double sumSq = 0.0;
        for (float v : embedding) {
            sumSq += (double) v * v;
        }
        assertEquals(1.0, sumSq, 1e-4, "Embedding must be unit-normalised (L2 norm ≈ 1)");
    }

    @Test
    void extractEmbedding_shouldReturn512DimensionalVectorFromStub() {
        float[] embedding = faceRecognitionService.extractEmbedding(SAMPLE_IMAGE_BASE64);
        assertEquals(512, embedding.length);
    }

    @Test
    void extractEmbedding_shouldStripDataUriPrefix() {
        String withPrefix = "data:image/jpeg;base64," + SAMPLE_IMAGE_BASE64;
        float[] withPrefixResult = faceRecognitionService.extractEmbedding(withPrefix);
        float[] withoutPrefixResult = faceRecognitionService.extractEmbedding(SAMPLE_IMAGE_BASE64);

        assertArrayEquals(withPrefixResult, withoutPrefixResult,
                "Data-URI prefix must be stripped before processing");
    }

    // -------------------------------------------------------------------------
    // Input validation
    // -------------------------------------------------------------------------

    @Test
    void extractEmbedding_shouldThrowWhenImageIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> faceRecognitionService.extractEmbedding(null));
    }

    @Test
    void extractEmbedding_shouldThrowWhenImageIsBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> faceRecognitionService.extractEmbedding("   "));
    }

    @Test
    void extractEmbedding_shouldThrowWhenBase64IsInvalid() {
        assertThrows(IllegalArgumentException.class,
                () -> faceRecognitionService.extractEmbedding("!!!not-valid-base64!!!"));
    }

    // -------------------------------------------------------------------------
    // cosineSimilarity: identical unit vectors → score ≈ 1.0
    // -------------------------------------------------------------------------

    @Test
    void cosineSimilarity_shouldReturnOneForIdenticalVectors() {
        float[] embedding = faceRecognitionService.extractEmbedding(SAMPLE_IMAGE_BASE64);
        double similarity = faceRecognitionService.cosineSimilarity(embedding, embedding);

        assertEquals(1.0, similarity, 1e-4,
                "Cosine similarity of a unit vector with itself must be ≈ 1.0");
    }

    @Test
    void cosineSimilarity_shouldExceedThresholdForSameImageRoundTrip() {
        float[] original = faceRecognitionService.extractEmbedding(SAMPLE_IMAGE_BASE64);
        String json = faceRecognitionService.embeddingToJson(original);
        float[] restored = faceRecognitionService.parseEmbedding(json);

        double similarity = faceRecognitionService.cosineSimilarity(original, restored);
        assertTrue(similarity >= 0.65,
                "Same image round-tripped through JSON must exceed the match threshold 0.65, got: " + similarity);
    }

    // -------------------------------------------------------------------------
    // External service mode
    // -------------------------------------------------------------------------

    @Test
    void extractEmbedding_shouldDelegateToExternalServiceWhenUrlConfigured() throws Exception {
        // Build a unit vector: only component[0] = 1.0 → already L2-normalised
        float[] serviceEmbedding = new float[512];
        serviceEmbedding[0] = 1.0f;
        String json = buildJsonArray(serviceEmbedding);

        setFaceServiceUrl("http://face-service.example.com");
        mockExchange(json);

        float[] result = faceRecognitionService.extractEmbedding(SAMPLE_IMAGE_BASE64);

        assertEquals(512, result.length);
        assertEquals(1.0f, result[0], 1e-4f,
                "When external service URL is set, result must come from the service");
    }

    @Test
    void extractEmbedding_shouldThrowWhenExternalServiceReturnsEmbeddingTooShort() throws Exception {
        // 4 values – below the 32-minimum
        String json = "[0.1, 0.2, 0.3, 0.4]";

        setFaceServiceUrl("http://face-service.example.com");
        mockExchange(json);

        assertThrows(IllegalArgumentException.class,
                () -> faceRecognitionService.extractEmbedding(SAMPLE_IMAGE_BASE64));
    }

    @Test
    void extractEmbedding_shouldAccept128DimensionalEmbeddingFromExternalService() throws Exception {
        float[] embedding128 = new float[128];
        embedding128[0] = 1.0f; // unit vector
        String json = buildJsonArray(embedding128);

        setFaceServiceUrl("http://face-service.example.com");
        mockExchange(json);

        float[] result = faceRecognitionService.extractEmbedding(SAMPLE_IMAGE_BASE64);
        assertEquals(128, result.length,
                "128-d embeddings from external service (e.g. face_recognition library) must be accepted");
    }

    @Test
    void extractEmbedding_shouldL2NormaliseEmbeddingFromExternalService() throws Exception {
        // Non-unit vector: norm = sqrt(3^2 + 4^2) = 5; after normalisation: [0.6, 0.8, 0, ...]
        float[] rawEmbedding = new float[128];
        rawEmbedding[0] = 3.0f;
        rawEmbedding[1] = 4.0f;
        String json = buildJsonArray(rawEmbedding);

        setFaceServiceUrl("http://face-service.example.com");
        mockExchange(json);

        float[] result = faceRecognitionService.extractEmbedding(SAMPLE_IMAGE_BASE64);

        // Verify unit length
        double sumSq = 0.0;
        for (float v : result) sumSq += (double) v * v;
        assertEquals(1.0, sumSq, 1e-4,
                "Embedding from external service must be L2-normalised to unit length");
        assertEquals(0.6f, result[0], 1e-4f);
        assertEquals(0.8f, result[1], 1e-4f);
    }

    @Test
    void extractEmbedding_shouldHandleStructuredResponseWithWarning() throws Exception {
        float[] embedding128 = new float[128];
        embedding128[0] = 1.0f;
        String embJson = buildJsonArray(embedding128);
        String json = "{\"embedding\":" + embJson + ",\"warning\":\"Face is small\"}";

        setFaceServiceUrl("http://face-service.example.com");
        mockExchange(json);

        float[] result = faceRecognitionService.extractEmbedding(SAMPLE_IMAGE_BASE64);
        assertEquals(128, result.length,
                "Structured response with 'embedding' field must be accepted");
    }

    // -------------------------------------------------------------------------
    // Embedding serialisation round-trip
    // -------------------------------------------------------------------------

    @Test
    void embeddingToJson_parseEmbedding_shouldRoundTripExactly() {
        float[] original = faceRecognitionService.extractEmbedding(SAMPLE_IMAGE_BASE64);
        String json = faceRecognitionService.embeddingToJson(original);
        float[] restored = faceRecognitionService.parseEmbedding(json);

        assertNotNull(json);
        assertArrayEquals(original, restored, 1e-4f,
                "Embedding must survive JSON round-trip without precision loss");
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private void setFaceServiceUrl(String url) throws Exception {
        Field field = FaceRecognitionService.class.getDeclaredField("faceServiceUrl");
        field.setAccessible(true);
        field.set(faceRecognitionService, url);
    }

    @SuppressWarnings("unchecked")
    private void mockExchange(String jsonBody) {
        ResponseEntity<String> responseEntity =
                new ResponseEntity<>(jsonBody, HttpStatus.OK);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);
    }

    private String buildJsonArray(float[] values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(values[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
