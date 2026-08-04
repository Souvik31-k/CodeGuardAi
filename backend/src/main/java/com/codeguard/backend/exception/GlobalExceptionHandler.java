package com.codeguard.backend.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RepositoryNotFoundException.class)
    public ResponseEntity<String> handleRepositoryNotfound(RepositoryNotFoundException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(RepositoryAlreadyExistsException.class)
    public ResponseEntity<String> handleRepositoryAlreadyExist(RepositoryAlreadyExistsException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        Map<String, String> err = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error -> err.put(error.getField(), error.getDefaultMessage()));

        return new ResponseEntity<>(err, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidWebhookSignatureException.class)
    public ResponseEntity<String> handleInvalidWebhookSignat(InvalidWebhookSignatureException e) {
        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(InvalidWebhookPayloadException.class)
    public ResponseEntity<String> handleInvalidWebhookPayload(InvalidWebhookPayloadException e) {
        return ResponseEntity.unprocessableContent().build();
    }

    @ExceptionHandler(LlmProviderException.class)
    public ResponseEntity<String> handleLlmproviderException(LlmProviderException e) {
        return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
