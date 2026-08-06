package com.codeguard.backend.dto.github.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GithubRepositoryDto {
    // Repository id
    private Long id;

    // Repository Name
    private String name;

    // Owner + Repository Name
    @JsonProperty("full_name")
    private String fullName;

    // Owner properties
    private GithubOwnerDto owner;
}
