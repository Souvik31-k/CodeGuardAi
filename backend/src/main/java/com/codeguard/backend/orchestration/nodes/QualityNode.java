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
import com.codeguard.backend.orchestration.prompt.QualityPromptBuilder;
import com.codeguard.backend.orchestration.state.ReviewState;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class QualityNode implements AsyncNodeAction<ReviewState> {

    private static final Logger log = LoggerFactory.getLogger(QualityNode.class);
    private static final long TIMEOUT_SECONDS = 10;

    private final QualityPromptBuilder promptBuilder;
    private final ObjectMapper mapper;
    private final LlmProvider provider;
    private final ExecutorService specialistExecutor;

    public QualityNode(QualityPromptBuilder promptBuilder, ObjectMapper mapper, LlmProvider provider,
            ExecutorService speacialistExecutor) {
        this.promptBuilder = promptBuilder;
        this.mapper = mapper;
        this.provider = provider;
        this.specialistExecutor = speacialistExecutor;
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(ReviewState state) {
        List<FileClassification> qualityFiles = state.classifications()
                .stream()
                .filter((classification) -> classification
                        .getCategory() == FileCategory.QUALITY)
                .toList();

        log.info("Recieved Quality files {} for Review Run Id {}", qualityFiles, state.reviewRunId());

        /**
         * No files classified for the Quality Node by the supervisor
         */
        if (qualityFiles.isEmpty()) {
            log.info("No Quality files found for Review Run Id: {}. Skipping Quality Agent",
                    state.reviewRunId());

            SpecialistResult result = new SpecialistResult(
                    AgentType.QUALITY,
                    SpecialistStatus.COMPLETED,
                    List.of(),
                    null);

            return CompletableFuture.completedFuture((Map.of(
                    ReviewState.QUALITY_RESULT,
                    result)));

        }

        List<ChangedFile> changedQualityFiles = state.changedFiles()
                .stream()
                .filter((changedFile) -> qualityFiles.stream()
                        .anyMatch((classification) -> classification.getFilePath()
                                .equals(changedFile.getFilePath())))
                .toList();

        LlmRequest request = promptBuilder.buildPrompt(state, changedQualityFiles);

        return CompletableFuture
                .supplyAsync(() -> provider.generate(request), specialistExecutor)
                .orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)

                .handle((response, exception) -> {

                    /**
                     * Llm call failed or timeout
                     */

                    if (exception != null) {

                        if (exception instanceof TimeoutException) {

                            log.error("Quality Node time out for Review Run Id {}",
                                    state.reviewRunId());

                            return new SpecialistResult(
                                    AgentType.QUALITY,
                                    SpecialistStatus.TIMEOUT,
                                    List.of(),
                                    "Quality Node time out.");
                        }

                        log.error("Quality Node Failed to generate Response for Review Run Id {}",
                                state.reviewRunId(), exception);

                        return new SpecialistResult(
                                AgentType.QUALITY,
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

                        log.info("Quality Node completed for Review Run Id {}",
                                state.reviewRunId());

                        return new SpecialistResult(
                                AgentType.QUALITY,
                                SpecialistStatus.COMPLETED,
                                findings,
                                null);

                    } catch (Exception e) {
                        log.error("Quality Node failed to parse Llm Response for Review Run Id {}",
                                state.reviewRunId());

                        return new SpecialistResult(
                                AgentType.QUALITY,
                                SpecialistStatus.FAILED,
                                List.of(),
                                "Failed to parse Quality Node Response" + e.getMessage());
                    }

                }).thenApply((result) -> Map.of(
                        ReviewState.QUALITY_RESULT,
                        result));
    }

    /**
     * Parsing the LLM response content to Agent Finding type
     * 
     * @param response
     * @return List<AgentFinding>
     * @throws Exception
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
        return parsed.getFindings();
    }

}
