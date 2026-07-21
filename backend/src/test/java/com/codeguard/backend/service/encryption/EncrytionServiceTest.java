package com.codeguard.backend.service.encryption;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class EncrytionServiceTest {
    public final EncryptionService encryptionService;

    EncrytionServiceTest(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Test
    public void checkencryptdecrypt() {
        String secret = "WebHookSecret";
        String encrypt = encryptionService.encrypt(secret);
        String decrypt = encryptionService.decrypt(encrypt);

        assertEquals(secret, decrypt);
    }
}
