package com.codeguard.backend.github.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.codeguard.backend.github.dto.GitHubChangedFileResponse;
import com.codeguard.backend.orchestration.model.ChangedFile;

@Component
public class GitHubMapper {

    public ChangedFile toChangedFile(GitHubChangedFileResponse response) {
        ChangedFile changedFile = new ChangedFile();
        changedFile.setFilePath(response.getFileName());
        changedFile.setPatch(response.getPatch());

        return changedFile;

    }

    public List<ChangedFile> convertGitHubChangedFileResponse(List<GitHubChangedFileResponse> response) {

        return response.stream()
                .map(this::toChangedFile)
                .toList();
    }
}
