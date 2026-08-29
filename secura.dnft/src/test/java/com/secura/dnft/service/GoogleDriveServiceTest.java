package com.secura.dnft.service;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GoogleDriveServiceTest {

    @Autowired
    private GoogleDriveService googleDriveService;

    private static String base64Image;

    private static final String FILE_NAME = "TEST_IMAGE_001";
    private static final String DOCUMENT_TYPE = "NOTICE";
    private static final String APPLICATION_NO = "TEST_IMAGE_001";
    private static final String FOLDER_NAME = "APRT001";

    @BeforeAll
    static void setup() throws Exception {

        Path imagePath = Path.of(
                "C:/Users/user/Desktop/Asit-Mohanty.jpg"
        );

        byte[] imageBytes = Files.readAllBytes(imagePath);

        base64Image = Base64.getEncoder()
                .encodeToString(imageBytes);
    }

    /**
     * Test Case 1:
     * Upload image to Google Drive.
     */
    @Test
    void uploadFileToDrive() {

        String uploadedPath =
                googleDriveService.uploadDataToDrive(
                        base64Image,
                        DOCUMENT_TYPE,
                        APPLICATION_NO,
                        FOLDER_NAME,"2054"
                );

        assertNotNull(
                uploadedPath,
                "Uploaded path should not be null"
        );

        assertFalse(
                uploadedPath.isBlank(),
                "Uploaded path should not be blank"
        );

        System.out.println(
                "Uploaded Path: " + uploadedPath
        );
    }

    /**
     * Test Case 2:
     * Read/download file from Google Drive.
     */
    @Test
    void readFileFromDrive() {

        // First upload the file so that this test is independent.
        String uploadedPath =
                googleDriveService.uploadDataToDrive(
                        base64Image,
                        DOCUMENT_TYPE,
                        APPLICATION_NO,
                        FOLDER_NAME,"2054"
                );

        assertNotNull(uploadedPath);

        // Read the uploaded file from Google Drive.
        String downloadedBase64 =
                googleDriveService.getFileFromDrive(
                        uploadedPath
                );

        assertNotNull(
                downloadedBase64,
                "Downloaded Base64 should not be null"
        );

        assertFalse(
                downloadedBase64.isBlank(),
                "Downloaded Base64 should not be blank"
        );

        // Verify that downloaded content is valid Base64.
        byte[] downloadedBytes =
                Base64.getDecoder()
                        .decode(downloadedBase64);

        assertNotNull(downloadedBytes);

        assertTrue(
                downloadedBytes.length > 0,
                "Downloaded file should contain data"
        );

        System.out.println(
                "Downloaded Base64 length: "
                        + downloadedBase64.length()
        );

        System.out.println(
                "Downloaded file size: "
                        + downloadedBytes.length
                        + " bytes"
        );
    }
}