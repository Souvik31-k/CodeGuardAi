package com.codeguard.backend.orchestration.nodes;

import java.util.List;
import java.util.Map;

import org.bsc.langgraph4j.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

import com.codeguard.backend.exception.LlmProviderException;
import com.codeguard.backend.llm.LlmProvider;
import com.codeguard.backend.llm.LlmRequest;
import com.codeguard.backend.llm.LlmResponse;
import com.codeguard.backend.orchestration.dto.SupervisorClassificationResponse;
import com.codeguard.backend.orchestration.model.FileClassification;
import com.codeguard.backend.orchestration.prompt.SupervisorPromptBuilder;
import com.codeguard.backend.orchestration.state.ReviewState;
import com.codeguard.backend.orchestration.state.ReviewState.ClassificationStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class SupervisorNode implements NodeAction<ReviewState> {

    private static final Logger log = LoggerFactory.getLogger(SupervisorNode.class);
    private final LlmProvider llmProvider;
    private final SupervisorPromptBuilder prompt;
    private final ObjectMapper mapper;

    public SupervisorNode(LlmProvider llmProvider, SupervisorPromptBuilder prompt, ObjectMapper mapper) {
        this.llmProvider = llmProvider;
        this.prompt = prompt;
        this.mapper = mapper;
    }

    @Override
    public Map<String, Object> apply(ReviewState state) throws Exception {
        LlmRequest request = prompt.buildPrompt(state);
        LlmResponse response;

        try {
            response = llmProvider.generate(request);
        } catch (LlmProviderException e) {

            log.error(
                    "LLM Provider failed for ReviewRun {}",
                    state.reviewRunId(),
                    e);

            return failure("LLM provider failed: " + e.getMessage());
        }

        // if the llm return blank or null response

        if (response == null || response.getContent() == null || response.getContent().isBlank()) {

            log.error(
                    "Supervisor returned an empty response for ReviewRunId {}",
                    state.reviewRunId());

            return failure("Supervisor returned an empty response.");

        }

        /**
         * Parsing the response content json to fetch the classification li
         */
        try {

            String cleanedJson = extractJson(response.getContent());

            SupervisorClassificationResponse classificationResponse = mapper.readValue(cleanedJson,
                    SupervisorClassificationResponse.class);

            List<FileClassification> classifications = classificationResponse.getClassifications();

            if (classifications == null || classifications.isEmpty()) {

                log.error("Supervisor returned no classifications for ReviewRun {} ", state.reviewRunId());

                return failure("Supervisor returned no classifications.");
            }

            return Map.of(
                    ReviewState.STATUS, ClassificationStatus.COMPLETED,
                    ReviewState.CLASSIFICATIONS, classifications);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse supervisor response for ReviewRun {}", state.reviewRunId(), e);

            return failure(e.getMessage());
        }
    }

    private Map<String, Object> failure(String reason) {
        return Map.of(
                ReviewState.STATUS, ClassificationStatus.FAILED,
                ReviewState.FAILURE_REASON, reason);
    }

    private String extractJson(String response) {

        if (response == null) {
            return null;
        }

        response = response.trim();

        if (response.startsWith("```json")) {
            response = response.substring(7);
        } else if (response.startsWith("```")) {
            response = response.substring(3);
        }

        if (response.endsWith("```")) {
            response = response.substring(0, response.length() - 3);
        }

        return response.trim();
    }

}
