package com.codeguard.backend.exception;

public class RepositoryAlreadyExistsException extends RuntimeException {
    public RepositoryAlreadyExistsException(String message) {
        super(message);
    }
}
