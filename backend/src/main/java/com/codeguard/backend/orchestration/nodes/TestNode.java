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
import com.codeguard.backend.orchestration.prompt.TestPromptBuilder;
import com.codeguard.backend.orchestration.state.ReviewState;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class TestNode implements AsyncNodeAction<ReviewState> {

        private static final Logger log = LoggerFactory.getLogger(TestNode.class);
        private static final long TIMEOUT_SECONDS = 300;
        private static final int MAX_BATCH_CHARS = 10000;

        private final TestPromptBuilder promptBuilder;
        private final ObjectMapper mapper;
        private final LlmProvider provider;
        private final ExecutorService specialistExecutor;

        public TestNode(LlmProvider provider, TestPromptBuilder promptBuilder, ObjectMapper mapper,
                        ExecutorService specialistExecutor) {
                this.provider = provider;
                this.promptBuilder = promptBuilder;
                this.mapper = mapper;
                this.specialistExecutor = specialistExecutor;
        }

        @Override
        public CompletableFuture<Map<String, Object>> apply(ReviewState state) {

                List<FileClassification> testFiles = state
                                .classifications()
                                .stream()
                                .filter((file) -> file.getCategory() == FileCategory.TEST)
                                .toList();

                log.info("Received Test files for Review Run Id {}", state.reviewRunId());

                /**
                 * No files classified for the Test Node by the supervisor
                 */
                if (testFiles.isEmpty()) {
                        log.info("No Test files found for Review Run Id: {}. Skipping Test Agent",
                                        state.reviewRunId());

                        SpecialistResult result = new SpecialistResult(
                                        AgentType.TEST,
                                        SpecialistStatus.COMPLETED,
                                        List.of(),
                                        null);

                        return CompletableFuture.completedFuture((Map.of(
                                        ReviewState.TEST_RESULT,
                                        result)));

                }

                List<ChangedFile> changedTestFile = state
                                .changedFiles()
                                .stream()
                                .filter((changedFile) -> testFiles.stream()
                                                .anyMatch((file) -> file.getFilePath()
                                                                .equals(changedFile.getFilePath())))
                                .toList();

                /*
                 * Split the changed files into size-bounded batch.
                 */
                List<List<ChangedFile>> batched = SpecialistBatcher.batch(changedTestFile, MAX_BATCH_CHARS);

                log.info(
                                "Test Node created {} batches for {} files"
                                                + " for Review Run Id {}",
                                batched.size(),
                                changedTestFile.size(),
                                state.reviewRunId());
                /*
                 * Execute batches sequentially.
                 */

                return CompletableFuture
                                .supplyAsync(() -> {

                                        List<AgentFinding> allFindings = new ArrayList<>();

                                        for (int i = 0; i < batched.size(); i++) {

                                                List<ChangedFile> batch = batched.get(i);

                                                log.info(
                                                                "Processing Test batch {}/{} "
                                                                                + "with {} files for Review Run {}",
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
                                                                        "Test batch {}/{} completed "
                                                                                        + "with {} findings",
                                                                        i + 1,
                                                                        batched.size(),
                                                                        findings.size());

                                                } catch (Exception e) {
                                                        throw new IllegalStateException(
                                                                        "Failed to parse Test batch "
                                                                                        + (i + 1)
                                                                                        + "/"
                                                                                        + batched.size(),
                                                                        e);
                                                }
                                        }

                                        return new SpecialistResult(
                                                        AgentType.TEST,
                                                        SpecialistStatus.COMPLETED,
                                                        allFindings,
                                                        null);

                                }, specialistExecutor)
                                .orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                                .handle((result, exception) -> {

                                        if (exception != null) {

                                                if (exception instanceof TimeoutException) {

                                                        log.error("Test Node timeout for Review Run Id: {}",
                                                                        state.reviewRunId());

                                                        return new SpecialistResult(
                                                                        AgentType.TEST,
                                                                        SpecialistStatus.TIMEOUT,
                                                                        List.of(),
                                                                        "Test Node time out.");
                                                }

                                                log.error("Test Node failed for Review Run Id: {}",
                                                                state.reviewRunId());

                                                return new SpecialistResult(
                                                                AgentType.TEST,
                                                                SpecialistStatus.FAILED,
                                                                List.of(),
                                                                exception.getMessage());
                                        }

                                        log.info(
                                                        "Test node completed for Review Run {} "
                                                                        + "with {} findings",
                                                        state.reviewRunId(),
                                                        result.getFindings().size());
                                        return result;

                                }).thenApply((result) -> Map.of(
                                                ReviewState.TEST_RESULT,
                                                result));
        }

        private List<AgentFinding> parseResponse(LlmResponse response) throws Exception {
                if (response == null || response.getContent() == null || response.getContent().isBlank()) {
                        throw new IllegalStateException("Llm returned invalid response");
                }

                SpecialistAnalysisResponse parsed = mapper.readValue(response.getContent(),
                                SpecialistAnalysisResponse.class);

                if (parsed.getFindings() == null || parsed.getFindings().isEmpty()) {
                        return List.of();
                }

                List<AgentFinding> findings = parsed.getFindings();

                for (AgentFinding finding : findings) {
                        if (finding.getSeverity() == null) {
                                log.warn("Finding '{}' missing severity, defaulting to MEDIUM", finding.getTitle());
                                finding.setSeverity(Severity.MEDIUM);
                        }
                }

                findings.forEach((finding) -> finding.setAgentType(AgentType.TEST));

                return findings;
        }

}
