package com.springshop.common.util;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 암호화/해싱/인코딩 유틸리티.
 */
public final class EncryptionUtils {

    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SHA_256 = "SHA-256";
    private static final String SHA_512 = "SHA-512";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private EncryptionUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * AES-256-GCM 암호화. key는 32바이트(256비트)여야 한다.
     */
    public static String encryptAES(String plaintext, String key) {
        if (plaintext == null) return null;
        if (key == null || key.length() < 32) {
            throw new IllegalArgumentException("Key must be at least 32 bytes");
        }
        try {
            byte[] keyBytes = key.substring(0, 32).getBytes(StandardCharsets.UTF_8);
            SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");

            byte[] iv = new byte[GCM_IV_LENGTH];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_GCM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // IV + Ciphertext 결합
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("AES encryption failed", e);
        }
    }

    public static String decryptAES(String ciphertextB64, String key) {
        if (ciphertextB64 == null) return null;
        if (key == null || key.length() < 32) {
            throw new IllegalArgumentException("Key must be at least 32 bytes");
        }
        try {
            byte[] keyBytes = key.substring(0, 32).getBytes(StandardCharsets.UTF_8);
            SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");

            byte[] combined = Base64.getDecoder().decode(ciphertextB64);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(AES_GCM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES decryption failed", e);
        }
    }

    public static String hashSHA256(String text) {
        return hash(text, SHA_256);
    }

    public static String hashSHA512(String text) {
        return hash(text, SHA_512);
    }

    private static String hash(String text, String algorithm) {
        if (text == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(algorithm + " hashing failed", e);
        }
    }

    public static String generateHMAC(String data, String secret) {
        if (data == null || secret == null) return null;
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(keySpec);
            byte[] result = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new RuntimeException("HMAC generation failed", e);
        }
    }

    public static boolean verifyHMAC(String data, String secret, String expected) {
        String actual = generateHMAC(data, secret);
        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8)
        );
    }

    public static String encodeBase64(String text) {
        if (text == null) return null;
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    public static String encodeBase64Url(String text) {
        if (text == null) return null;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    public static String decodeBase64(String b64) {
        if (b64 == null) return null;
        return new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8);
    }

    public static byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    public static String generateSalt(int length) {
        return Base64.getEncoder().encodeToString(generateRandomBytes(length));
    }
}
