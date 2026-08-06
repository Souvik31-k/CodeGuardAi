package com.codeguard.backend.github.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.codeguard.backend.dto.github.webhook.GIthubPullRequestEvent;
import com.codeguard.backend.github.client.GitHubClient;
import com.codeguard.backend.github.dto.GitHubChangedFileResponse;
import com.codeguard.backend.github.mapper.GitHubMapper;
import com.codeguard.backend.model.CodeRepository;
import com.codeguard.backend.orchestration.model.ChangedFile;

@Service
public class GitHubPullRequestService {
    private final GitHubClient client;
    private final GitHubTokenService tokenService;
    private final GitHubMapper mapper;

    public GitHubPullRequestService(GitHubClient client, GitHubTokenService tokenService, GitHubMapper mapper) {
        this.client = client;
        this.tokenService = tokenService;
        this.mapper = mapper;
    }

    public List<ChangedFile> getChangedFiles(GIthubPullRequestEvent event, CodeRepository repository) {

        String owner = event.getRepository().getOwner().getLogin();
        String repo = event.getRepository().getName();
        Long pullRequestNumber = event.getPullRequest().getNumber();
        String accessToken = tokenService.getAccessToken(repository);

        List<GitHubChangedFileResponse> response = client.getPullRequestFiles(owner, repo, pullRequestNumber,
                accessToken);
        return mapper.convertGitHubChangedFileResponse(response);
    }
}
