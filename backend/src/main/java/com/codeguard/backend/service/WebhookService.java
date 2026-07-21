package com.codeguard.backend.service;

import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeguard.backend.dto.github.webhook.GIthubPullRequestEvent;
import com.codeguard.backend.enums.GithubEvent;
import com.codeguard.backend.enums.PullRequestAction;
import com.codeguard.backend.exception.InvalidWebhookPayloadException;
import com.codeguard.backend.exception.RepositoryNotFoundException;
import com.codeguard.backend.model.CodeRepository;
import com.codeguard.backend.repository.CodeRepositoryRepository;
import com.codeguard.backend.service.encryption.EncryptionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class WebhookService {
    // Telling where the log in comming from. Right Now it is comming from the
    // WebhookService
    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final SignatureVerifier signatureVerifier;
    private final ObjectMapper mapper;
    private final CodeRepositoryRepository codeRepository;
    private final EncryptionService encryptionService;

    WebhookService(ObjectMapper mapper, CodeRepositoryRepository codeRepository, SignatureVerifier signatureVerifier,
            EncryptionService encryptionService) {
        this.mapper = mapper;
        this.codeRepository = codeRepository;
        this.signatureVerifier = signatureVerifier;
        this.encryptionService = encryptionService;
    }

    public void handleWebhook(String event, String signature, String delivery, String payload) {

        try {
            // Deserialize Payload once.
            GIthubPullRequestEvent webhookEvent = mapper.readValue(payload, GIthubPullRequestEvent.class);

            // Check if repository or repositoryId is null - catch the error
            if (webhookEvent.getRepository() == null || webhookEvent.getRepository().getId() == null) {
                throw new RepositoryNotFoundException("Repository not found");
            }

            Long repositoryId = webhookEvent.getRepository().getId();

            CodeRepository codeRepo = codeRepository.findByGithubRepoId(repositoryId)
                    .orElseThrow(() -> new RepositoryNotFoundException("Repository not registered."));

            // Decrypt the encrypted webhook fetched from db before hmac conversion
            String plainWebhookSecret = encryptionService.decrypt(codeRepo.getWebhookSecretEncrypted());

            // verify the signature using payload + decrypted webhook.
            boolean validSignature = signatureVerifier
                    .verify(signature,
                            payload,
                            plainWebhookSecret);
            if (!validSignature) {
                throw new IllegalArgumentException("Invalid gitHub webhook Signature");
            }

            if (!codeRepo.isActive()) {
                throw new RepositoryNotFoundException("Repository is InActive.");
            }

            // Todo:
            // Persist github delivery for duplicate delivery detection.

            GithubEvent eve = GithubEvent.fromheader(event);
            if (eve == null) {
                log.info("Ignoring Unsupported github Event '{}' ", event);
                return;
            }

            switch (eve) {

                case PING:
                    handlePing();
                    break;

                case PULL_REQUEST:
                    handlePullRequest(webhookEvent, codeRepo);
                    break;

                default:
                    log.info("Ignoring Github event '{}' ", event);
            }
        } catch (JsonProcessingException e) {
            throw new InvalidWebhookPayloadException("Invalid webhook Payload.", e);
        }

    }

    private void handlePing() {
        log.info("Recieved GitHub webhook Ping.");
    }

    private void handlePullRequest(GIthubPullRequestEvent event, CodeRepository repo) {
        PullRequestAction action = event.getAction();
        if (action == null) {
            log.warn("Pull request action is missing.");
            return;
        }
        String fullName = event.getRepository().getFullName();
        Long githubRepoId = event.getRepository().getId();
        Integer pullRequestNumber = event.getPullRequest().getNumber();
        // String headSha = event.getPullRequest().getHead().getSha();
        // String baseSha = event.getPullRequest().getBase().getSha();
        log.info("Processing {} PR #{} for repository {} (GitHub ID: {})", action, pullRequestNumber, fullName,
                githubRepoId);

        // Todo:
        // Create ReviewRun
        // Persist ReviewRun
        // Trigger SupervisorAgent

        switch (action) {
            case OPENED:
                handleOpened(event, repo);
                break;
            case SYNCHRONIZE:
                handleSynchronize(event, repo);
                break;
            case REOPENED:
                handleReOpened(event, repo);
                break;
            case CLOSED:
                log.info("No Review Run required for Pull Request Action '{}' ", action);
                break;
            default:
                log.info("Ignoring pull request action '{}'", action);
        }
    }

    private void handleReOpened(GIthubPullRequestEvent event, CodeRepository repo) {
        log.info("Handle pull request reopened");

        // Todo:
        // Create Review Run
        // Save Review Run
        // Trigger Supervisor Agent
    }

    private void handleSynchronize(GIthubPullRequestEvent event, CodeRepository repo) {
        log.info("Handle pull request synchronize");

        // Todo:
        // Create Review Run
        // Save Review Run
        // Trigger Supervisor Agent
    }

    private void handleOpened(GIthubPullRequestEvent event, CodeRepository repo) {
        log.info("Handle pull request opened");

        // Todo:
        // Create Review Run
        // Save Review Run
        // Trigger Supervisor Agent
    }

}
