package com.codeguard.backend.orchestration.prompt;

import java.util.StringJoiner;

import org.springframework.stereotype.Component;

import com.codeguard.backend.llm.LlmRequest;
import com.codeguard.backend.orchestration.model.ChangedFile;
import com.codeguard.backend.orchestration.state.ReviewState;

@Component
public class SupervisorPromptBuilder {
    public LlmRequest buildPrompt(ReviewState state) {
        String systemPrompt = """
                You are the Supervisor Agent of the CodeGuard AI Platform.

                Your task is to classify each changed file into exactly one category.

                Allowed categories are:
                - SECURITY
                - TEST
                - PERFORMANCE
                - DOCUMENTATION

                Return ONLY valid JSON.

                Example:

                {
                  "classifications": [
                    {
                      "filePath":"src/UserService.java",
                      "category":"SECURITY"
                    }
                  ]
                }

                Do not include explanations.
                """;

        StringJoiner changedFiles = new StringJoiner("\n");

        for (ChangedFile file : state.getChangedFiled()) {
            changedFiles.add(file.getFilePath());
        }

        String userPrompt = """
                Review Run ID: %d

                Pull Request Number: %d

                Changed Files:

                %s
                """.formatted(
                state.getReviewRunId(),
                state.getPullRequestNumber(),
                changedFiles);

        LlmRequest request = new LlmRequest();
        request.setSystemPrompt(systemPrompt);
        request.setUserPrompt(userPrompt);
        request.setTemperature(0.0);

        return request;
    }
}
