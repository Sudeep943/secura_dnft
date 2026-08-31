package com.secura.dnft.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.sourceforge.tess4j.Tesseract;

@Configuration
public class OcrService {
	
	@Bean
	public Tesseract getTesseractInstance() {
        Tesseract tesseract = new Tesseract();

        try {
            // 1. Create a temporary folder on the cloud server to act as the "datapath"
            Path tempTessDataDir = Files.createTempDirectory("tessdata_cloud");
            
            // 2. Define the path where the file will be copied inside the temp folder
            Path tempTrainedData = tempTessDataDir.resolve("eng.traineddata");

            // 3. Read eng.traineddata from the root of src/main/resources
            try (InputStream in = getClass().getResourceAsStream("/eng.traineddata")) {
                if (in == null) {
                    throw new RuntimeException("eng.traineddata not found in resources!");
                }
                // 4. Copy the stream to the temporary file
                Files.copy(in, tempTrainedData, StandardCopyOption.REPLACE_EXISTING);
            }

            // 5. Point Tesseract to the temporary directory containing the copied file
            tesseract.setDatapath(tempTessDataDir.toAbsolutePath().toString());
            tesseract.setLanguage("eng");

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Tesseract data path", e);
        }

        return tesseract;
    }

}
