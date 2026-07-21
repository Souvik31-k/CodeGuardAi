package com.codeguard.backend.dto.response;

import com.codeguard.backend.model.CodeRepository.Severity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryResponse {
    private Long repoId;
    private Long githubRepoId;
    private String repoName;
    private String userLogin;
    private Severity minimumSeverity;
    private boolean isActive;

}
