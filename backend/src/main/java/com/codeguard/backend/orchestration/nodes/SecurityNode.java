package com.codeguard.backend.orchestration.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.codeguard.backend.llm.LlmProvider;
import com.codeguard.backend.llm.LlmRequest;
import com.codeguard.backend.llm.LlmResponse;
import com.codeguard.backend.enums.AgentType;
import com.codeguard.backend.enums.Severity;
import com.codeguard.backend.orchestration.batching.SpecialistBatcher;
import com.codeguard.backend.orchestration.dto.SpecialistAnalysisResponse;
import com.codeguard.backend.orchestration.model.AgentFinding;
import com.codeguard.backend.orchestration.model.ChangedFile;
import com.codeguard.backend.orchestration.model.FileCategory;
import com.codeguard.backend.orchestration.model.FileClassification;
import com.codeguard.backend.orchestration.model.SpecialistResult;
import com.codeguard.backend.orchestration.model.SpecialistResult.SpecialistStatus;
import com.codeguard.backend.orchestration.prompt.SecurityPromptBuilder;
import com.codeguard.backend.orchestration.state.ReviewState;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class SecurityNode implements AsyncNodeAction<ReviewState> {
        private static final Logger log = LoggerFactory.getLogger(SecurityNode.class);
        private static final long TIMEOUT_SECONDS = 90;
        private static final int MAX_BATCH_CHARS = 10000;

        private final SecurityPromptBuilder promptBuilder;
        private final LlmProvider provider;
        private final ObjectMapper mapper;
        private final ExecutorService specialistExecutor;

        public SecurityNode(SecurityPromptBuilder promptBuilder, LlmProvider provider, ObjectMapper mapper,
                        ExecutorService specialistExecutor) {
                this.promptBuilder = promptBuilder;
                this.provider = provider;
                this.mapper = mapper;
                this.specialistExecutor = specialistExecutor;
        }

        @Override
        public CompletableFuture<Map<String, Object>> apply(ReviewState state) {

                List<FileClassification> securityFiles = state.classifications()
                                .stream()
                                .filter((classification) -> classification.getCategory() == FileCategory.SECURITY)
                                .toList();

                log.info(
                                "Recieved Security Files {} for ReviewRun {}",
                                securityFiles.size(),
                                state.reviewRunId());

                /**
                 * No files classified for the Security Node by the supervisor
                 */
                if (securityFiles.isEmpty()) {
                        log.info("No Security files found for Review Run Id: {}. Skipping Security Agent",
                                        state.reviewRunId());

                        SpecialistResult result = new SpecialistResult(
                                        AgentType.SECURITY,
                                        SpecialistStatus.COMPLETED,
                                        List.of(),
                                        null);

                        return CompletableFuture.completedFuture((Map.of(
                                        ReviewState.SECURITY_RESULT,
                                        result)));

                }

                List<ChangedFile> changedFiles = state.changedFiles()
                                .stream()
                                .filter(changedFile -> securityFiles.stream()
                                                .anyMatch(classification -> classification.getFilePath()
                                                                .equals(changedFile.getFilePath())))
                                .toList();
                /*
                 * Split the changed files into size-bounded batch.
                 */
                List<List<ChangedFile>> batched = SpecialistBatcher.batch(changedFiles, MAX_BATCH_CHARS);

                log.info(
                                "Security Node created {} batched for {} files "
                                                + "for Review Run {}",
                                batched.size(),
                                changedFiles.size(),
                                state.reviewRunId());

                /*
                 * Execute the batches sequentially
                 */
                return CompletableFuture
                                .supplyAsync(() -> {

                                        List<AgentFinding> allFindings = new ArrayList<>();

                                        for (int i = 0; i < batched.size(); i++) {

                                                List<ChangedFile> batch = batched.get(i);

                                                log.info(
                                                                "Processing Security Node for batch {}/{}"
                                                                                + " with {} files for Review Run {}",
                                                                i + 1,
                                                                batched.size(),
                                                                batch.size(),
                                                                state.reviewRunId());

                                                LlmRequest request = promptBuilder.buildPrompt(state, batch);

                                                LlmResponse response = provider.generate(request);

                                                try {
                                                        List<AgentFinding> findings = parseResponse(response);

                                                        allFindings.addAll(findings);

                                                        log.info(
                                                                        "Security batch {}/{} completed "
                                                                                        + "with {} findings",
                                                                        i + 1,
                                                                        batched.size(),
                                                                        findings.size());

                                                } catch (Exception e) {
                                                        throw new IllegalStateException(
                                                                        "Failed to parse Security batch"
                                                                                        + (i + 1)
                                                                                        + "/"
                                                                                        + batched.size(),
                                                                        e);
                                                }
                                        }

                                        return new SpecialistResult(
                                                        AgentType.SECURITY,
                                                        SpecialistStatus.COMPLETED,
                                                        allFindings,
                                                        null);

                                }, specialistExecutor)
                                .orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                                .handle((result, exception) -> {

                                        /*
                                         * LLM call failed or timed out.
                                         */
                                        if (exception != null) {

                                                if (exception instanceof TimeoutException) {

                                                        log.error(
                                                                        "Security Node timed out for ReviewRun {}",
                                                                        state.reviewRunId());

                                                        return new SpecialistResult(
                                                                        AgentType.SECURITY,
                                                                        SpecialistStatus.TIMEOUT,
                                                                        List.of(),
                                                                        "Security Node timed out.");
                                                }

                                                log.error(
                                                                "Security Node failed for ReviewRun {}",
                                                                state.reviewRunId(),
                                                                exception);

                                                return new SpecialistResult(
                                                                AgentType.SECURITY,
                                                                SpecialistStatus.FAILED,
                                                                List.of(),
                                                                exception.getMessage());
                                        }

                                        log.info(
                                                        "Quality Node completed for Review Run {} "
                                                                        + "with {} findings",
                                                        state.reviewRunId(),
                                                        result.getFindings().size());

                                        return result;

                                })

                                /*
                                 * AsyncNodeAction requires:
                                 *
                                 * CompletableFuture<Map<String, Object>>
                                 */
                                .thenApply(result -> Map.of(
                                                ReviewState.SECURITY_RESULT,
                                                result));
        }

        private List<AgentFinding> parseResponse(LlmResponse response) throws Exception {
                if (response == null || response.getContent() == null || response.getContent().isBlank()) {

                        throw new IllegalStateException(
                                        "Security Agent returned an empty response");
                }

                SpecialistAnalysisResponse parsed = mapper.readValue(response.getContent(),
                                SpecialistAnalysisResponse.class);

                if (parsed.getFindings() == null) {
                        return List.of();
                }

                List<AgentFinding> findings = parsed.getFindings();

                for (AgentFinding finding : findings) {

                        if (finding.getSeverity() == null) {
                                log.warn("Finding '{}' missing severity, defaulting to CRITICAL", finding.getTitle());
                                finding.setSeverity(Severity.CRITICAL);
                        }
                }

                findings.forEach((finding) -> finding.setAgentType(AgentType.SECURITY));

                return parsed.getFindings();
        }

}
