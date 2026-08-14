package com.codeguard.backend.service;

import org.springframework.stereotype.Service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeguard.backend.dto.github.webhook.GIthubPullRequestEvent;
import com.codeguard.backend.enums.GithubEvent;
import com.codeguard.backend.enums.PullRequestAction;
import com.codeguard.backend.exception.InvalidWebhookPayloadException;
import com.codeguard.backend.exception.InvalidWebhookSignatureException;
import com.codeguard.backend.exception.RepositoryNotFoundException;
import com.codeguard.backend.github.service.GitHubPullRequestService;
import com.codeguard.backend.model.CodeRepository;
import com.codeguard.backend.model.ReviewRun;
import com.codeguard.backend.orchestration.model.ChangedFile;
import com.codeguard.backend.orchestration.service.ReviewGraphService;
import com.codeguard.backend.orchestration.state.ReviewState;
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
    private final ReviewGraphService reviewGraphService;
    private final GitHubPullRequestService gitHubPullRequestService;

    WebhookService(ObjectMapper mapper, CodeRepositoryRepository codeRepository, SignatureVerifier signatureVerifier,
            EncryptionService encryptionService, ReviewRunService reviewRunService,
            ReviewGraphService reviewGraphService, GitHubPullRequestService gitHubPullRequestService) {
        this.mapper = mapper;
        this.codeRepository = codeRepository;
        this.signatureVerifier = signatureVerifier;
        this.encryptionService = encryptionService;
        this.reviewRunService = reviewRunService;
        this.reviewGraphService = reviewGraphService;
        this.gitHubPullRequestService = gitHubPullRequestService;
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
                handleOpened(event, repo);
                break;
            // Updating the Pull Request with fresh code
            case SYNCHRONIZE:
                handleSynchronize(event, repo);
                break;
            // Reopening an old Pull Request, that was closed without being merged
            case REOPENED:
                handleReOpened(event, repo);
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

    private void handleOpened(GIthubPullRequestEvent event, CodeRepository repo) {
        log.info("Creating a new Pull Request");

        triggerReview(event, repo);
    }

    private void handleSynchronize(GIthubPullRequestEvent event, CodeRepository repo) {
        log.info("Handle pull request synchronize");

        triggerReview(event, repo);
    }

    private void handleReOpened(GIthubPullRequestEvent event, CodeRepository repo) {
        log.info("Handle Reopened Pull Request");

        triggerReview(event, repo);
    }

    private void triggerReview(GIthubPullRequestEvent event, CodeRepository repository) {

        Long pullRequestNumber = event.getPullRequest().getNumber();
        String headSha = event.getPullRequest().getHead().getSha();

        ReviewRun reviewRun = reviewRunService.createReviewRun(
                pullRequestNumber,
                headSha,
                repository);

        try {

            List<ChangedFile> changedFiles = gitHubPullRequestService.getChangedFiles(
                    event,
                    repository);

            if (changedFiles.isEmpty()) {

                log.info(
                        "Skipping ReviewRun {} because no changed files were found.",
                        reviewRun.getReviewRunId());

                // Todo (Phase 4):
                // reviewRunService.markCompleted(reviewRun);

                return;
            }
            /**
             * Triggering the Supervisor Node
             */
            ReviewState finalState = reviewGraphService.startReview(
                    reviewRun,
                    changedFiles);

            if (finalState.status() == ReviewState.ClassificationStatus.FAILED) {

                log.error(
                        "Review Graph failed for ReviewRun {}. Reason: {}",
                        reviewRun.getReviewRunId(),
                        finalState.failureReason());

                // Todo (Phase 4):
                // reviewRunService.markFailed(reviewRun, finalState.failureReason());

                return;
            }

            log.info(
                    "Review Graph completed successfully for ReviewRun {}",
                    reviewRun.getReviewRunId());

            // Todo (Phase 4):
            // reviewRunService.markCompleted(reviewRun);

        } catch (Exception e) {

            log.error(
                    "Failed to execute review workflow for ReviewRun {}",
                    reviewRun.getReviewRunId(),
                    e);

            // Todo (Phase 4):
            // reviewRunService.markFailed(reviewRun, e.getMessage());
        }
    }

}
