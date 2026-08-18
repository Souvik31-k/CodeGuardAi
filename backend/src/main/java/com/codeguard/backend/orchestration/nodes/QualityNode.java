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

import com.codeguard.backend.enums.AgentType;
import com.codeguard.backend.enums.Severity;
import com.codeguard.backend.llm.LlmProvider;
import com.codeguard.backend.llm.LlmRequest;
import com.codeguard.backend.llm.LlmResponse;
import com.codeguard.backend.orchestration.batching.SpecialistBatcher;
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
    private static final long TIMEOUT_SECONDS = 90;
    private static final int MAX_BATCH_CHARS = 10000;

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

        /*
         * Split the files into size-bounded batches.
         */
        List<List<ChangedFile>> batched = SpecialistBatcher.batch(changedQualityFiles, MAX_BATCH_CHARS);

        log.info("Quality Node created {} batches for {} files in Review Run {}.",
                batched.size(),
                changedQualityFiles.size(),
                state.reviewRunId());

        return CompletableFuture
                .supplyAsync(() -> {

                    List<AgentFinding> allFindings = new ArrayList<>();

                    for (int i = 0; i < batched.size(); i++) {

                        List<ChangedFile> batch = batched.get(i);

                        log.info(
                                "Quality node processing batch {}/{}"
                                        + "with {} files for Review Run {}",
                                i + 1,
                                batched.size(),
                                batch.size(),
                                state.reviewRunId());

                        LlmRequest request = promptBuilder.buildPrompt(state, batch);

                        LlmResponse response = provider.generate(request);

                        /*
                         * Llm call succeed
                         * parse the response
                         */

                        try {
                            List<AgentFinding> findings = parseResponse(response);

                            allFindings.addAll(findings);

                            log.info(
                                    "Quality batch {}/{} completed with {} findings "
                                            + "for Review Run {}",
                                    i + 1,
                                    batched.size(),
                                    findings.size(),
                                    state.reviewRunId());

                        } catch (Exception e) {

                            throw new IllegalStateException(
                                    "Failed to parse Quality batch "
                                            + (i + 1)
                                            + "/"
                                            + batched.size(),
                                    e);
                        }
                    }

                    return new SpecialistResult(
                            AgentType.QUALITY,
                            SpecialistStatus.COMPLETED,
                            allFindings,
                            null);

                }, specialistExecutor)
                .orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)

                .handle((result, exception) -> {

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

                        log.error("Quality Node failed for Review Run {}",
                                state.reviewRunId(),
                                exception);

                        return new SpecialistResult(
                                AgentType.QUALITY,
                                SpecialistStatus.FAILED,
                                List.of(),
                                exception.getMessage());
                    }
                    log.info("Quality Node completed for Review Run  {} with {} findings {}",
                            state.reviewRunId(),
                            result.getFindings().size());

                    return result;

                }).thenApply((result) -> Map.of(
                        ReviewState.QUALITY_RESULT,
                        result));
    }

    /**
     * Parsing the LLM response content to Agent Finding type
     */
    private List<AgentFinding> parseResponse(LlmResponse response) throws Exception {

        if (response == null
                || response.getContent() == null
                || response.getContent().isBlank()) {

            throw new IllegalStateException(
                    "LLM returned invalid response, cannot be parsed.");
        }

        String json = cleanJsonResponse(response.getContent());

        SpecialistAnalysisResponse parsed = mapper.readValue(
                json,
                SpecialistAnalysisResponse.class);

        if (parsed.getFindings() == null
                || parsed.getFindings().isEmpty()) {

            return List.of();
        }

        List<AgentFinding> findings = parsed.getFindings();

        for (AgentFinding finding : findings) {

            if (finding.getSeverity() == null) {

                log.warn(
                        "QUALITY finding '{}' returned without severity. "
                                + "Defaulting severity to MEDIUM.",
                        finding.getTitle());

                finding.setSeverity(Severity.MEDIUM);
            }

            // Agent identity comes from the node, not the LLM.
            finding.setAgentType(AgentType.QUALITY);
        }

        return findings;
    }

    private String cleanJsonResponse(String content) {

        String cleaned = content.trim();

        if (cleaned.startsWith("```json")) {

            cleaned = cleaned.substring(7).trim();

        } else if (cleaned.startsWith("```")) {

            cleaned = cleaned.substring(3).trim();

        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }

        return cleaned;
    }

}
