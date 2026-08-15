package com.codeguard.backend.orchestration.prompt;

import java.util.List;
import java.util.StringJoiner;

import org.springframework.stereotype.Component;

import com.codeguard.backend.llm.LlmRequest;
import com.codeguard.backend.orchestration.model.ChangedFile;
import com.codeguard.backend.orchestration.state.ReviewState;

@Component
public class SecurityPromptBuilder {

  public LlmRequest buildPrompt(ReviewState state, List<ChangedFile> securityFiles) {

    String systemPrompt = """
        You are the Security Agent of the CodeGuard Ai Platform.

        Analyze the provided Code changes for security vulnerablities.

        Look specifically for:

        - Authentication vulnerabilities
        - Authorization vulnerabilities
        - SQL injection
        - XSS
        - CSRF
        - Hardcoded secrets
        - Password handling issues
        - JWT/security configuration issues
        - Input validation vulnerabilities
        - Insecure cryptographic usage
        - OAuth/security configuration problems

        Base your findings primarily on the actual code changes
        contained in the patches.

        Return ONLY valid JSON.

        Response format:
        {
          "findings": [
            {
              "agentType":SECURITY,
              "severity": "HIGH",
              "title": "...",
              "filePath": "...",
              "lineNumber": 42,
              "details": {
                "rule": "...",
                "evidence": "...",
                "recommendation": "..."
              }
            }
          ]
        }

        If no security problems are found, return:

        {
          "findings": []
        }

        Allowed severity values:
        CRITICAL, HIGH, MEDIUM, LOW

        Do not wrap the JSON in markdown.
        Do not include explanations outside the JSON.

        """;

    StringJoiner files = new StringJoiner("\n\n");
    for (ChangedFile file : securityFiles) {
      files.add("""
          File:
          %s

          Patch:
          %s
          """.formatted(
          file.getFilePath(),
          file.getPatch() == null
              ? "(No file patch available)"
              : file.getPatch()));
    }

    String userPrompt = """
        Review Run Id: %d

        Pull Request Number: %d

        Security-Related changed files:

        %s
        """.formatted(
        state.reviewRunId(),
        state.pullRequestNumber(),
        files);

    LlmRequest request = new LlmRequest();

    request.setSystemPrompt(systemPrompt);
    request.setUserPrompt(userPrompt);
    request.setTemperature(0.0);

    return request;

  }
}
