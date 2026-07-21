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
public class UpdateRepositoryRequest {

    @NotBlank(message = "Repository name is required")
    private String repoName;
    @NotNull
    private boolean isActive;
    @NotNull(message = "Minimum severity is required")
    private Severity minimumSeverity;
}
