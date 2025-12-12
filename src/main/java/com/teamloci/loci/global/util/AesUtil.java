package com.teamloci.loci.global.util;

import com.teamloci.loci.global.error.CustomException;
import com.teamloci.loci.global.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class AesUtil {

    private final SecretKeySpec secretKeySpec;
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final SecureRandom secureRandom = new SecureRandom();

    public AesUtil(@Value("${jwt.secret-key}") String secretKey) {
        byte[] key = secretKey.getBytes(StandardCharsets.UTF_8);
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 32);
            this.secretKeySpec = new SecretKeySpec(key, "AES");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public String encrypt(String plainText) {
        if (plainText == null) return null;
        try {
            byte[] ivBytes = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(ivBytes);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, ivBytes);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, spec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ivBytes) + ":" + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null) return null;
        try {
            String[] parts = encryptedText.split(":");
            if (parts.length != 2) throw new CustomException(ErrorCode.DECRYPT_FAILED);

            byte[] ivBytes = Base64.getDecoder().decode(parts[0]);
            byte[] cipherBytes = Base64.getDecoder().decode(parts[1]);

            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, ivBytes);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, spec);

            return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.DECRYPT_FAILED);
        }
    }
    public String hash(String plainText) {
        if (plainText == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(plainText.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new CustomException(ErrorCode.HASHING_FAILED);
        }
    }
}