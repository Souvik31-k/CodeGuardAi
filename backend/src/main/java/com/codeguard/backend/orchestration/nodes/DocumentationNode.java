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

import com.codeguard.backend.enums.AgentType;
import com.codeguard.backend.enums.Severity;
import com.codeguard.backend.llm.LlmProvider;
import com.codeguard.backend.llm.LlmRequest;
import com.codeguard.backend.llm.LlmResponse;
import com.codeguard.backend.orchestration.dto.SpecialistAnalysisResponse;
import com.codeguard.backend.orchestration.model.AgentFinding;
import com.codeguard.backend.orchestration.model.ChangedFile;
import com.codeguard.backend.orchestration.model.FileCategory;
import com.codeguard.backend.orchestration.model.FileClassification;
import com.codeguard.backend.orchestration.model.SpecialistResult;
import com.codeguard.backend.orchestration.model.SpecialistResult.SpecialistStatus;
import com.codeguard.backend.orchestration.prompt.DocumentPromptBuilder;
import com.codeguard.backend.orchestration.state.ReviewState;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class DocumentationNode implements AsyncNodeAction<ReviewState> {

        private static final Logger log = LoggerFactory.getLogger(DocumentationNode.class);
        private static final long TIMEOUT_SECONDS = 10;

        private final DocumentPromptBuilder promptBuilder;
        private final ObjectMapper mapper;
        private final LlmProvider provider;
        private final ExecutorService specialistExecutor;

        public DocumentationNode(DocumentPromptBuilder promptBuilder, ObjectMapper mapper, LlmProvider provider,
                        ExecutorService specialistExecutor) {
                this.promptBuilder = promptBuilder;
                this.mapper = mapper;
                this.provider = provider;
                this.specialistExecutor = specialistExecutor;
        }

        @Override
        public CompletableFuture<Map<String, Object>> apply(ReviewState state) {
                List<FileClassification> documentationFiles = state.classifications()
                                .stream()
                                .filter((classification) -> classification
                                                .getCategory() == FileCategory.DOCUMENTATION)
                                .toList();

                log.info("Recieved Documentation files {} for Review Run Id {}", documentationFiles,
                                state.reviewRunId());

                /**
                 * No files classified for the Documentation Node by the supervisor
                 */
                if (documentationFiles.isEmpty()) {
                        log.info("No Document files found for Review Run Id: {}. Skipping Documentation Agent",
                                        state.reviewRunId());

                        SpecialistResult result = new SpecialistResult(
                                        AgentType.DOCUMENTATION,
                                        SpecialistStatus.COMPLETED,
                                        List.of(),
                                        null);

                        return CompletableFuture.completedFuture((Map.of(
                                        ReviewState.DOCUMENTATION_RESULT,
                                        result)));

                }

                List<ChangedFile> changedDocFiles = state.changedFiles()
                                .stream()
                                .filter((changedFile) -> documentationFiles.stream()
                                                .anyMatch((classification) -> classification.getFilePath()
                                                                .equals(changedFile.getFilePath())))
                                .toList();

                LlmRequest request = promptBuilder.buildPrompt(state, changedDocFiles);

                return CompletableFuture
                                .supplyAsync(() -> provider.generate(request), specialistExecutor)
                                .orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)

                                .handle((response, exception) -> {

                                        /**
                                         * Llm call failed or timeout
                                         */

                                        if (exception != null) {

                                                if (exception instanceof TimeoutException) {

                                                        log.error("Document Node time out for Review Run Id {}",
                                                                        state.reviewRunId());

                                                        return new SpecialistResult(
                                                                        AgentType.DOCUMENTATION,
                                                                        SpecialistStatus.TIMEOUT,
                                                                        List.of(),
                                                                        "Document Node time out.");
                                                }

                                                log.error("Document Node Failed to generate Response for Review Run Id {}",
                                                                state.reviewRunId(), exception);

                                                return new SpecialistResult(
                                                                AgentType.DOCUMENTATION,
                                                                SpecialistStatus.FAILED,
                                                                List.of(),
                                                                exception.getMessage());
                                        }

                                        /*
                                         * Llm call succeed
                                         * parse the response
                                         */

                                        try {
                                                List<AgentFinding> findings = parseRespose(response);

                                                log.info("Document Node completed for Review Run Id {}",
                                                                state.reviewRunId());

                                                return new SpecialistResult(
                                                                AgentType.DOCUMENTATION,
                                                                SpecialistStatus.COMPLETED,
                                                                findings,
                                                                null);

                                        } catch (Exception e) {
                                                log.error("Document Node failed to parse Llm Response for Review Run Id {}",
                                                                state.reviewRunId());

                                                return new SpecialistResult(
                                                                AgentType.DOCUMENTATION,
                                                                SpecialistStatus.FAILED,
                                                                List.of(),
                                                                "Failed to parse Documentation Node Response"
                                                                                + e.getMessage());
                                        }

                                }).thenApply((result) -> Map.of(
                                                ReviewState.DOCUMENTATION_RESULT,
                                                result));
        }

        /**
         * Parsing the LLM response content to Agent Finding type
         */
        private List<AgentFinding> parseRespose(LlmResponse response) throws Exception {
                if (response == null || response.getContent() == null || response.getContent().isBlank()) {
                        throw new IllegalStateException("Llm returned invalid response, cannot be parsed.");
                }
                SpecialistAnalysisResponse parsed = mapper.readValue(response.getContent(),
                                SpecialistAnalysisResponse.class);

                if (parsed.getFindings() == null || parsed.getFindings().isEmpty()) {
                        return List.of();
                }

                List<AgentFinding> findings = parsed.getFindings();

                for (AgentFinding finding : findings) {
                        if (finding.getSeverity() == null) {
                                log.warn("Finding '{}' missing severity, defaulting to LOW", finding.getTitle());
                                finding.setSeverity(Severity.LOW);
                        }
                }

                findings.forEach((finding) -> finding.setAgentType(AgentType.DOCUMENTATION));

                return findings;
        }

}
