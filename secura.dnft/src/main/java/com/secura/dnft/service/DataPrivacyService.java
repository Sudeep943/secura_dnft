package com.secura.dnft.service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
public class DataPrivacyService {
	private static final String ALGO = "AES";
	
	 @Value("${privacy.secrect.key}")
	 private String secretKey;
	 
// 
//	 public static void main(String[] args) {
//		try {
//			   String secretKey = "1234567890123456"; // must be 16 chars for AES-128
//		        String originalText = "HelloWorld";
//
//		        String encrypted = encrypt(originalText, secretKey);
//		        System.out.println("Encrypted: " + encrypted);
//
//		        String decrypted = decrypt(encrypted, secretKey);
//		        System.out.println("Decrypted: " + decrypted);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//	}
	
	public static SecretKey generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGO);
        keyGen.init(128); // AES-128
        return keyGen.generateKey();
    }
	

    // 🔐 Encrypt
    public String encrypt(String data) throws Exception {
        SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(), ALGO);

        Cipher cipher = Cipher.getInstance(ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    // 🔓 Decrypt
    public String decrypt(String encryptedData) throws Exception {
        SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(), ALGO);

        Cipher cipher = Cipher.getInstance(ALGO);
        cipher.init(Cipher.DECRYPT_MODE, key);

        byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);

        return new String(decryptedBytes);
    }
}
