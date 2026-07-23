package com.codeguard.backend.service;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Service;

@Service
public class SecretGenerator {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // 32 random bytes = 256 bits of entropy
    private static final int SECRET_LENGTH = 32;

    public String generate() {
        byte[] secret = new byte[SECRET_LENGTH];
        SECURE_RANDOM.nextBytes(secret);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(secret);
    }
}
