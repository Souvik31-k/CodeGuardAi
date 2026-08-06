package com.codeguard.backend.github.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "github")
public class GitHubProperties {

    /**
     * example(used by the restClient to fetch the baseurl)
     * https://api.github.com/
     */
    private String baseUrl;

    /**
     * example(used by the restClient while sending the request)
     * X-github-Api-Version
     * 2022-11-1
     */
    private String apiVersion;

    private String accessToken;
}
