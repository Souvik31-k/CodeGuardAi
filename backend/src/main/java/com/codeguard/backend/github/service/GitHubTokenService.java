package com.codeguard.backend.github.service;

import org.springframework.stereotype.Service;

import com.codeguard.backend.github.config.GitHubProperties;
import com.codeguard.backend.model.CodeRepository;

@Service
public class GitHubTokenService {
    private final GitHubProperties properties;

    public GitHubTokenService(GitHubProperties properties) {
        this.properties = properties;
    }

    public String getAccessToken(CodeRepository repository) {
        return properties.getAccessToken();
    }
}
