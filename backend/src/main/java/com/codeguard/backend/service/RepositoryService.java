package com.codeguard.backend.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.codeguard.backend.dto.request.CreateRepositoryRequest;
import com.codeguard.backend.dto.request.UpdateRepositoryRequest;
import com.codeguard.backend.dto.response.CreateRepositoryResponse;
import com.codeguard.backend.dto.response.RepositoryResponse;
import com.codeguard.backend.exception.RepositoryAlreadyExistsException;
import com.codeguard.backend.exception.RepositoryNotFoundException;
import com.codeguard.backend.model.CodeRepository;
import com.codeguard.backend.repository.CodeRepositoryRepository;
import com.codeguard.backend.service.encryption.EncryptionService;

@Service
public class RepositoryService {
    private final CodeRepositoryRepository repository;
    private final EncryptionService encryptionService;
    private final SecretGenerator secretGenerator;

    RepositoryService(CodeRepositoryRepository repository, EncryptionService encryptionService,
            SecretGenerator secretGenerator) {
        this.repository = repository;
        this.encryptionService = encryptionService;
        this.secretGenerator = secretGenerator;
    }

    public CreateRepositoryResponse createRepository(CreateRepositoryRequest request) {
        if (repository.existsByGithubRepoId(request.getGithubRepoId())) {
            throw new RepositoryAlreadyExistsException(
                    "Repository with Github Id " + request.getGithubRepoId() + " already exists.");
        }
        CodeRepository codeRepo = new CodeRepository();
        codeRepo.setGithubRepoId(request.getGithubRepoId());
        codeRepo.setRepoName(request.getRepoName());
        codeRepo.setUserLogin(request.getUserLogin());
        codeRepo.setMinimumSeverity(request.getMinimumSeverity());
        codeRepo.setActive(true);
        String webhookSecret = secretGenerator.generate();
        String encrypted = encryptionService.encrypt(webhookSecret);
        codeRepo.setWebhookSecretEncrypted(encrypted);

        CodeRepository savedRepo = repository.save(codeRepo);

        CreateRepositoryResponse response = new CreateRepositoryResponse();
        response.setRepoId(savedRepo.getRepoId());
        response.setRepoName(savedRepo.getRepoName());
        response.setGeneratedWebhookSecret(webhookSecret);

        return response;
    }

    public List<RepositoryResponse> getAllRepositories() {
        List<CodeRepository> repositories = repository.findAll();
        List<RepositoryResponse> responses = new ArrayList<>();
        for (CodeRepository repo : repositories) {
            responses.add(mapToRepositoryResponse(repo));
        }
        return responses;
    }

    public RepositoryResponse getRepositoryById(Long id) {
        CodeRepository repo = repository.findById(id).orElse(null);
        if (repo != null) {
            return mapToRepositoryResponse(repo);
        } else {
            throw new RepositoryNotFoundException("Repository Not Found");
        }
    }

    public RepositoryResponse updateRepository(Long id, UpdateRepositoryRequest request) {
        CodeRepository repo = repository.findById(id).orElse(null);
        if (repo != null) {
            repo.setRepoName(request.getRepoName());
            repo.setActive(request.isActive());
            repo.setMinimumSeverity(request.getMinimumSeverity());
            CodeRepository saveRepository = repository.save(repo);
            return mapToRepositoryResponse(saveRepository);
        } else {
            throw new RepositoryNotFoundException("Repository not found to update");
        }
    }

    public RepositoryResponse deactivateRepository(Long id) {
        CodeRepository repo = repository.findById(id).orElseThrow(() -> new RepositoryNotFoundException(
                "Repository not found with id: " + id));

        repo.setActive(false);
        CodeRepository saveRepo = repository.save(repo);
        return mapToRepositoryResponse(saveRepo);
    }

    private RepositoryResponse mapToRepositoryResponse(CodeRepository repo) {
        RepositoryResponse response = new RepositoryResponse();

        response.setRepoId(repo.getRepoId());
        response.setGithubRepoId(repo.getGithubRepoId());
        response.setRepoName(repo.getRepoName());
        response.setUserLogin(repo.getUserLogin());
        response.setMinimumSeverity(repo.getMinimumSeverity());
        response.setActive(repo.isActive());

        return response;
    }

}
