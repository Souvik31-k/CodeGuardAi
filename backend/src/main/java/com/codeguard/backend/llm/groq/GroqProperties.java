package com.codeguard.backend.llm.groq;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "llm.groq")
public class GroqProperties {
    private String apiKey;

    private String model;

    private String baseUrl;
}
