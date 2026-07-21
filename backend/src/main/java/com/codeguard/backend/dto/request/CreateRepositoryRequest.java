package com.codeguard.backend.dto.request;

import com.codeguard.backend.model.CodeRepository.Severity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateRepositoryRequest {
    @NotNull(message = "GitHub Repository ID is required")
    private Long githubRepoId;
    @NotBlank(message = "Repository name is required.")
    private String repoName;
    @NotBlank(message = "User login is required.")
    private String userLogin;
    @NotNull(message = "minimum severity is required")
    private Severity minimumSeverity;
}
