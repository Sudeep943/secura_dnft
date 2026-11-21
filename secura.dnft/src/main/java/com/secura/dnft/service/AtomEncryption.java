package com.secura.dnft.service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public final class AtomEncryption {

    private static final int ITERATIONS = 65536;
    private static final int KEY_SIZE = 256;

    private static final byte[] IV = {
            0, 1, 2, 3, 4, 5, 6, 7,
            8, 9, 10, 11, 12, 13, 14, 15
    };

    private AtomEncryption() {
    }

    private static SecretKeySpec generateKey(String key)
            throws Exception {

        byte[] salt = key.getBytes(StandardCharsets.UTF_8);

        PBEKeySpec spec = new PBEKeySpec(
                key.toCharArray(),
                salt,
                ITERATIONS,
                KEY_SIZE
        );

        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");

        SecretKey secretKey = factory.generateSecret(spec);

        return new SecretKeySpec(secretKey.getEncoded(), "AES");
    }

    public static String encrypt(String plainText, String key)
            throws Exception {

        SecretKeySpec secretKey = generateKey(key);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        cipher.init(
                Cipher.ENCRYPT_MODE,
                secretKey,
                new IvParameterSpec(IV)
        );

        byte[] encrypted = cipher.doFinal(
                plainText.getBytes(StandardCharsets.UTF_8)
        );

        return toHex(encrypted);
    }

    public static String decrypt(String encryptedText, String key)
            throws Exception {

        SecretKeySpec secretKey = generateKey(key);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey,
                new IvParameterSpec(IV)
        );

        byte[] decrypted = cipher.doFinal(fromHex(encryptedText));

        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private static String toHex(byte[] bytes) {

        StringBuilder result = new StringBuilder();

        for (byte b : bytes) {
            result.append(String.format("%02X", b & 0xFF));
        }

        return result.toString();
    }

    private static byte[] fromHex(String hex) {

        if (hex == null || hex.isBlank() || hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Invalid encrypted hex data");
        }

        byte[] result = new byte[hex.length() / 2];

        for (int i = 0; i < hex.length(); i += 2) {
            result[i / 2] = (byte) Integer.parseInt(
                    hex.substring(i, i + 2), 16
            );
        }

        return result;
    }
}