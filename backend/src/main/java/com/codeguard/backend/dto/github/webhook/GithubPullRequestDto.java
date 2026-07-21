package com.codeguard.backend.dto.github.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GithubPullRequestDto {

    private Integer number;

    private GithubBranchDto head;

    private GithubBranchDto base;
}
