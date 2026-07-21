package com.codeguard.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateRepositoryResponse {
    private Long repoId;
    private String repoName;
    private String generatedWebhookSecret;
}
