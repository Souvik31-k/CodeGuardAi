package com.codeguard.backend.exception;

public class InvalidWebhookPayloadException extends RuntimeException {
    public InvalidWebhookPayloadException(String message, Exception e) {
        super(message, e);
    }
}
