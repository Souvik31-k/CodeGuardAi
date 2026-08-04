package com.codeguard.backend.exception;

public class LlmProviderException extends RuntimeException {

    public LlmProviderException(String message, Exception e) {
        super(message, e);
    }

    public LlmProviderException(String message) {
        super(message);
    }

}
