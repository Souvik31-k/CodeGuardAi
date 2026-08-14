package com.codeguard.backend.github.client;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.codeguard.backend.exception.GitHubClientExcpetion;
import com.codeguard.backend.github.config.GitHubProperties;
import com.codeguard.backend.github.dto.GitHubChangedFileResponse;

@Component
public class GitHubClient {
        private final RestClient restClient;
        private final GitHubProperties properties;

        public GitHubClient(@Qualifier("gitHubRestClient") RestClient restClient, GitHubProperties properties) {
                this.restClient = restClient;
                this.properties = properties;
        }

        public List<GitHubChangedFileResponse> getPullRequestFiles(String owner, String repository,
                        Long pullRequestNumber,
                        String accessToken) {

                GitHubChangedFileResponse[] response;
                try {
                        response = restClient.get()
                                        .uri(properties.getBaseUrl()
                                                        + "/repos/{owner}/{repo}/pulls/{pullNumber}/files?per_page=100",
                                                        owner,
                                                        repository,
                                                        pullRequestNumber)
                                        .header(HttpHeaders.AUTHORIZATION,
                                                        "Bearer " + accessToken)
                                        .header(HttpHeaders.ACCEPT,
                                                        "application/vnd.github+json")
                                        .header("X-GitHub-Api-Version",
                                                        properties.getApiVersion())
                                        .accept(MediaType.APPLICATION_JSON)
                                        .retrieve()
                                        .body(GitHubChangedFileResponse[].class);

                        return response == null
                                        ? List.of()
                                        : Arrays.asList(response);

                } catch (RestClientException e) {
                        throw new GitHubClientExcpetion("Failed to fetch changed files from GitHub",
                                        e);
                }

        }
}
