package com.secura.dnft.security;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class AuthCryptoUtil {

    private final AuthCryptoProperties properties;

    public AuthCryptoUtil(AuthCryptoProperties properties) {
        this.properties = properties;
    }

    public String encrypt(String plainText) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(properties.getKeyBase64().trim());
            byte[] ivBytes = Base64.getDecoder().decode(properties.getIvBase64().trim());

            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance(properties.getTransformation());
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to encrypt auth field", ex);
        }
    }

    public String decrypt(String encryptedBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(properties.getKeyBase64().trim());
            byte[] ivBytes = Base64.getDecoder().decode(properties.getIvBase64().trim());
            byte[] cipherBytes = Base64.getDecoder().decode(encryptedBase64.trim());

            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance(properties.getTransformation());
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to decrypt auth field", ex);
        }
    }
}