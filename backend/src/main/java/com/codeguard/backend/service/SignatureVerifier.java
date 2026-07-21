package com.codeguard.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

@Service
public class SignatureVerifier {
    public boolean verify(String githubSignature, String payload, String webhookSecret) {
        if (githubSignature == null || !githubSignature.startsWith("sha256="))
            return false;
        String signature = generateSignature(payload, webhookSecret);
        return MessageDigest.isEqual(
                signature.getBytes(StandardCharsets.UTF_8),
                githubSignature.substring(7).getBytes(StandardCharsets.UTF_8));
    }

    private String generateSignature(String payload, String webhookSecret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(key);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to Generate HmacSignature");
        }

    }
}
