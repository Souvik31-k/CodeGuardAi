package com.codeguard.backend.orchestration.nodes;

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
        private static final long TIMEOUT_SECONDS = 10;

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

                LlmRequest request = promptBuilder.buildPrompt(state, changedFiles);

                return CompletableFuture
                                .supplyAsync(() -> provider.generate(request), specialistExecutor)
                                .orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                                .handle((response, exception) -> {

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

                                        /*
                                         * LLM call succeeded.
                                         * Now parse the response.
                                         */
                                        try {

                                                List<AgentFinding> findings = parseResponse(response);

                                                log.info(
                                                                "SecurityNode completed for ReviewRun {}. {} findings.",
                                                                state.reviewRunId(),
                                                                findings.size());

                                                return new SpecialistResult(
                                                                AgentType.SECURITY,
                                                                SpecialistStatus.COMPLETED,
                                                                findings,
                                                                null);

                                        } catch (Exception e) {

                                                log.error(
                                                                "Failed to parse Security Agent response for ReviewRun {}",
                                                                state.reviewRunId(),
                                                                e);

                                                return new SpecialistResult(
                                                                AgentType.SECURITY,
                                                                SpecialistStatus.FAILED,
                                                                List.of(),
                                                                "Failed to parse Security Agent response: "
                                                                                + e.getMessage());
                                        }
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
