package com.secura.dnft;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;

import com.secura.dnft.security.AuthCryptoProperties;
import com.secura.dnft.security.AuthCryptoUtil;

public class TestMain {


//	auth.crypto.key-base64=U2VjdXJhTG9naW5LZXlBRVMyNTZWYWx1ZTEyMzQ1Njc=
//			auth.crypto.iv-base64=U2VjdXJhSW5pdFZlYzEyMw==
//			auth.crypto.transformation=AES/CBC/PKCS5Padding
//	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		String s=encrypt("upi://pay?pa=payeeVpa@handle&pn=Payee%20Name&tid=TRANSACTIONID123&tr=10141%20Common%20Area%20Ma&tn=Transaction%20Note&am=100.00&cu=INR");
		System.out.println("Encrypted Value: "+s);
		System.out.println("Decrypted Value: "+decrypt(s));

		
		
	}
	
	
	public static String encrypt(String plainText) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode("U2VjdXJhTG9naW5LZXlBRVMyNTZWYWx1ZTEyMzQ1Njc=");
            byte[] ivBytes = Base64.getDecoder().decode("U2VjdXJhSW5pdFZlYzEyMw==");

            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to encrypt auth field", ex);
        }
    }

    public static String decrypt(String encryptedBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode("U2VjdXJhTG9naW5LZXlBRVMyNTZWYWx1ZTEyMzQ1Njc=");
            byte[] ivBytes = Base64.getDecoder().decode("U2VjdXJhSW5pdFZlYzEyMw==");
            byte[] cipherBytes = Base64.getDecoder().decode(encryptedBase64.trim());

            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to decrypt auth field", ex);
        }
    }

}
