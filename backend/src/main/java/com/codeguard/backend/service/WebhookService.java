package com.codeguard.backend.service;

import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeguard.backend.dto.github.webhook.GIthubPullRequestEvent;
import com.codeguard.backend.enums.GithubEvent;
import com.codeguard.backend.enums.PullRequestAction;
import com.codeguard.backend.exception.InvalidWebhookPayloadException;
import com.codeguard.backend.exception.InvalidWebhookSignatureException;
import com.codeguard.backend.exception.RepositoryNotFoundException;
import com.codeguard.backend.model.CodeRepository;
import com.codeguard.backend.model.ReviewRun;
import com.codeguard.backend.repository.CodeRepositoryRepository;
import com.codeguard.backend.service.encryption.EncryptionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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
    private final ReviewRunService reviewRunService;

    WebhookService(ObjectMapper mapper, CodeRepositoryRepository codeRepository, SignatureVerifier signatureVerifier,
            EncryptionService encryptionService, ReviewRunService reviewRunService) {
        this.mapper = mapper;
        this.codeRepository = codeRepository;
        this.signatureVerifier = signatureVerifier;
        this.encryptionService = encryptionService;
        this.reviewRunService = reviewRunService;
    }

    public void handleWebhook(String event, String signature, String delivery, String payload) {

        GithubEvent githubEvent = GithubEvent.fromheader(event);
        if (githubEvent == null) {
            log.info("Ignoring Unsupported github Event '{}' ", event);
            return;
        }

        try {
            // Deserialize Payload once.
            JsonNode root = mapper.readTree(payload);
            JsonNode repositoryNode = root.path("repository");

            // Check if repository node is missing - catch the error
            if (repositoryNode.isMissingNode()) {
                throw new RepositoryNotFoundException("Repository not found.");
            }

            Long repositoryId = repositoryNode.path("id").asLong();

            if (repositoryId == 0L) {
                throw new RepositoryNotFoundException("Repository not found.");
            }

            CodeRepository codeRepo = codeRepository.findByGithubRepoId(repositoryId)
                    .orElseThrow(() -> new RepositoryNotFoundException("Repository not registered."));

            // Decrypt the encrypted webhook fetched from db before hmac conversion
            String plainWebhookSecret = encryptionService.decrypt(codeRepo.getWebhookSecretEncrypted());

            // verify the signature using payload + decrypted webhook.
            boolean validSignature = signatureVerifier
                    .verify(signature,
                            payload,
                            plainWebhookSecret);
            // if the signate is not validated.
            if (!validSignature) {

                throw new InvalidWebhookSignatureException("Invalid gitHub webhook Signature");
            }

            if (!codeRepo.isActive()) {
                log.info("Ignoring Webhook for Inactive Repository {}", repositoryId);
                return;
            }

            // Handle ping only after the signature is verified.

            if (githubEvent == GithubEvent.PING) {

                handlePing();

                return;
            }

            // Todo:
            // Persist github delivery for duplicate delivery detection.
            if (githubEvent == GithubEvent.PULL_REQUEST) {
                GIthubPullRequestEvent webhookEvent = mapper.readValue(payload, GIthubPullRequestEvent.class);
                handlePullRequest(webhookEvent, codeRepo);
            }
        } catch (JsonProcessingException e) {
            throw new InvalidWebhookPayloadException("Invalid webhook Payload.", e);
        }

    }

    private void handlePing() {
        log.info("Received GitHub webhook Ping.");
    }

    private void handlePullRequest(GIthubPullRequestEvent event, CodeRepository repo) {
        PullRequestAction action = event.getAction();
        if (action == null) {
            log.warn("Pull request action is missing.");
            return;
        }
        String fullName = event.getRepository().getFullName();
        Long githubRepoId = event.getRepository().getId();
        Long pullRequestNumber = event.getPullRequest().getNumber();
        String headSha = event.getPullRequest().getHead().getSha();
        // String baseSha = event.getPullRequest().getBase().getSha();
        log.info("Processing {} PR #{} for repository {} (GitHub ID: {})", action, pullRequestNumber, fullName,
                githubRepoId);

        switch (action) {
            // creation of a brand new Pull Request
            case OPENED:
                handleOpened(pullRequestNumber, headSha, repo);
                break;
            // Updating the Pull Request with fresh code
            case SYNCHRONIZE:
                handleSynchronize(pullRequestNumber, headSha, repo);
                break;
            // Reopening an old Pull Request, that was closed without being merged
            case REOPENED:
                handleReOpened(pullRequestNumber, headSha, repo);
                break;
            // two scenario - check pullrequest.merged key
            // if true - Cannot be Reopened, since already merged with the source branch.
            // if false - Unmerged- the Pr was rejected , abandoned , or closed without
            // merging.
            case CLOSED:
                log.info("No Review Run required for Pull Request Action '{}' ", action);
                break;
            default:
                log.info("Ignoring pull request action '{}'", action);
        }
    }

    private void handleOpened(Long pullRequestNumber, String headSha, CodeRepository repo) {
        log.info("Creating a new Pull Request");
        ReviewRun reviewRun = reviewRunService.createReviewRun(pullRequestNumber, headSha, repo);

        // Todo:
        // Trigger Supervisor Agent
    }

    private void handleSynchronize(Long pullRequestNumber, String headSha, CodeRepository repo) {
        log.info("Handle pull request synchronize");

        ReviewRun reviewRun = reviewRunService.createReviewRun(pullRequestNumber, headSha, repo);
        // Todo:
        // Create Review Run
        // Save Review Run
        // Trigger Supervisor Agent
    }

    private void handleReOpened(Long pullRequestNumber, String headSha, CodeRepository repo) {
        log.info("Handle Reopened Pull Request");
        ReviewRun reviewRun = reviewRunService.createReviewRun(pullRequestNumber, headSha, repo);
        // Todo:
        // Create Review Run
        // Save Review Run
        // Trigger Supervisor Agent
    }

}
