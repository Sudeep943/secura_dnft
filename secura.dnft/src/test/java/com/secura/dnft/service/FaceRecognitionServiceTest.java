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
    void extractEmbedding_shouldReturn512DimensionalVector() {
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
        float[] serviceEmbedding = new float[512];
        serviceEmbedding[0] = 1.0f; // non-zero so it's distinct

        setFaceServiceUrl("http://face-service.example.com");
        when(restTemplate.postForObject(anyString(), any(), eq(float[].class)))
                .thenReturn(serviceEmbedding);

        float[] result = faceRecognitionService.extractEmbedding(SAMPLE_IMAGE_BASE64);

        assertArrayEquals(serviceEmbedding, result,
                "When external service URL is set, result must come from the service");
    }

    @Test
    void extractEmbedding_shouldThrowWhenExternalServiceReturnsWrongDimension() throws Exception {
        float[] wrongDimension = new float[128]; // not 512

        setFaceServiceUrl("http://face-service.example.com");
        when(restTemplate.postForObject(anyString(), any(), eq(float[].class)))
                .thenReturn(wrongDimension);

        assertThrows(IllegalArgumentException.class,
                () -> faceRecognitionService.extractEmbedding(SAMPLE_IMAGE_BASE64));
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
}
