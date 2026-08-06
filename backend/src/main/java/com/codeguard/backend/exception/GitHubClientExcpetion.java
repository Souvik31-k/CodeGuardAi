package com.codeguard.backend.exception;

public class GitHubClientExcpetion extends RuntimeException {
    public GitHubClientExcpetion(String message) {
        super(message);
    }

    public GitHubClientExcpetion(String message, Throwable cause) {
        super(message, cause);
    }
}
