package com.codeguard.backend.dto.github.webhook;

import com.codeguard.backend.enums.PullRequestAction;
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
@JsonIgnoreProperties(ignoreUnknown = true) // to ignore field from the payload that is not defined.
public class GIthubPullRequestEvent {
    private PullRequestAction action;
    private GithubRepositoryDto repository;

    @JsonProperty("pull_request")
    private GithubPullRequestDto pullRequest;

    private GithubSenderDto sender;

}
