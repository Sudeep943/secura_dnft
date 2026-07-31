package com.secura.dnft.service;

import java.security.SecureRandom;

public class CreditNoteUtiltyService {

    private static final String ALPHA_NUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOTAL_LENGTH = 10;
	
	 public  String generateCreditNoteNo(String input) {
	        if (input == null) {
	            input = "";
	        }
	        // Remove all special characters and spaces
	        String cleaned = input.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
	        // Truncate if more than 10 characters
	        if (cleaned.length() > TOTAL_LENGTH) {
	            return cleaned.substring(0, TOTAL_LENGTH);
	        }
	        StringBuilder builder = new StringBuilder(cleaned);
	        int remaining = TOTAL_LENGTH - cleaned.length();
	        for (int i = 0; i < remaining; i++) {
	            builder.append(ALPHA_NUMERIC.charAt(RANDOM.nextInt(ALPHA_NUMERIC.length())));
	        }
	        return builder.toString();
	    }
}
