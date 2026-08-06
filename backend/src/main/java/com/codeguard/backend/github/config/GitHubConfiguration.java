package com.codeguard.backend.github.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GitHubProperties.class)
public class GitHubConfiguration {
}
