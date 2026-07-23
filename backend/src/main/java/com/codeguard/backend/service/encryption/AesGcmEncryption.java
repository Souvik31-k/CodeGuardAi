package com.codeguard.backend.service.encryption;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AesGcmEncryption implements EncryptionService {

    @Value("${security.encryption.key}") // directly injects value from the env file(DotEnv library)- Refer to
                                         // DotenvConfiguration File.
    private String encryptionKey;

    @Override
    public String encrypt(String plaintext) {
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[12];
            SecureRandom securerandom = new SecureRandom();
            securerandom.nextBytes(iv);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, getKey(), gcmSpec);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to encrypt webhook secret", e);
        }
    }

    @Override
    public String decrypt(String ciphertext) {
        byte[] combined = Base64.getDecoder().decode(ciphertext);
        byte[] iv = new byte[12];

        System.arraycopy(combined, 0, iv, 0, iv.length);

        byte[] ciphertxt = new byte[combined.length - iv.length];
        System.arraycopy(combined, iv.length, ciphertxt, 0, ciphertxt.length);

        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getKey(), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(ciphertxt), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrpty webhook secret", e);
        }
    }

    private SecretKey getKey() {
        byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "AES-256 key must be exactly 32 bytes.");
        }
        return new SecretKeySpec(keyBytes, "AES");
    }

}
