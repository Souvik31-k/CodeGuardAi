package com.codeguard.backend.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GitHubChangedFileResponse {
    @JsonProperty("filename")
    private String fileName;

    private String status;
    private Integer additions;
    private Integer deletions;
    private Integer changes;
    private String patch;
}
