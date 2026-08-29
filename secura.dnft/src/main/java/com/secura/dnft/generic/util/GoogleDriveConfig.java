package com.secura.dnft.generic.util;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

@Configuration
public class GoogleDriveConfig {


    private static final String APPLICATION_NAME = "DN Fairytale";

    private static final String SERVICE_ACCOUNT_FILE =
            "securadnft-68d6dbb43f8a.json";

    
    @Bean
    public Drive driveService()
            throws IOException, GeneralSecurityException {

        // Get credentials automatically from Cloud Run
//        GoogleCredentials credentials =
//                GoogleCredentials.getApplicationDefault()
//                        .createScoped(
//                                Collections.singleton(
//                                        DriveScopes.DRIVE)).createDelegated("contact@dnfaoa.org");
        
    	InputStream inputStream = GoogleDriveConfig.class
    	        .getClassLoader()
    	        .getResourceAsStream("securadnft-aac25c98848a.json");

    	if (inputStream == null) {
    	    throw new FileNotFoundException(
    	            "securadnft-aac25c98848a.json not found in classpath"
    	    );
    	}

    	GoogleCredentials credentials =
    	        GoogleCredentials.fromStream(inputStream)
    	                .createScoped(
    	                        Collections.singleton(DriveScopes.DRIVE)
    	                ).createDelegated("contact@dnfaoa.org");

        // Convert credentials into HTTP authentication
        HttpRequestInitializer requestInitializer =
                new HttpCredentialsAdapter(credentials);

        // Create authenticated Google Drive client
        return new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                requestInitializer)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}