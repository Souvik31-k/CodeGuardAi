package com.codeguard.backend.orchestration.nodes;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

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
public class SupervisorNode {

    private static final Logger log = LoggerFactory.getLogger(SupervisorNode.class);
    private final LlmProvider llmProvider;
    private final SupervisorPromptBuilder prompt;
    private final ObjectMapper mapper;

    public SupervisorNode(LlmProvider llmProvider, SupervisorPromptBuilder prompt, ObjectMapper mapper) {
        this.llmProvider = llmProvider;
        this.prompt = prompt;
        this.mapper = mapper;
    }

    public ReviewState execute(ReviewState state) {
        LlmRequest request = prompt.buildPrompt(state);
        LlmResponse response = llmProvider.generate(request);

        // if the llm return blank or null response

        if (response == null || response.getContent() == null || response.getContent().isBlank()) {
            state.setStatus(ClassificationStatus.FAILED);
            state.setFailureReason("LLM returned empty response");
            log.error(
                    "Supervisor returned an empty response for ReviewRunId {}",
                    state.getReviewRunId());

            return state;
        }

        // Extract the classification from the response.
        try {
            SupervisorClassificationResponse classificationResponse = mapper.readValue(
                    response.getContent(),
                    SupervisorClassificationResponse.class);

            List<FileClassification> classifications = classificationResponse.getClassifications();

            if (classifications == null) {
                state.setStatus(ClassificationStatus.FAILED);
                state.setFailureReason("Supervisor returned no classification.");
                return state;
            }

            state.setClassifications(classifications);
            state.setStatus(ClassificationStatus.COMPLETED);
            state.setFailureReason(null);

        } catch (JsonProcessingException e) {
            state.setStatus(ClassificationStatus.FAILED);
            state.setFailureReason(e.getMessage());
            log.error("Failed to parse supervisor response for ReviewRun {}", state.getReviewRunId(), e);
        }

        return state;
    }

}
