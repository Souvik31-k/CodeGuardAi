package com.codeguard.backend.service.encryption;

import org.springframework.stereotype.Service;

@Service
public interface EncryptionService {
    String encrypt(String plaintext);

    String decrypt(String ciphertext);
}
