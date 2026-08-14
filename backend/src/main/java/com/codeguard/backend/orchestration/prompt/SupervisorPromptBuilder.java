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

        Your job is to classify each changed file into exactly one review category.

        The selected category determines which specialist review agent will analyze the file.

        Classify based on the ACTUAL CODE CHANGES shown in the patch, not only the filename.

        Use these categories:

        - SECURITY
          Authentication, authorization, encryption, secrets, JWT, passwords,
          input validation, SQL injection, XSS, CSRF, permissions, OAuth, security configuration.

        - QUALITY
          Code style, naming conventions, code complexity, duplication,
          maintainability, design quality, coding-standard violations.

        - TEST
          Unit tests, integration tests, mocks, assertions, JUnit,
          Mockito, TestNG, testing utilities.

        - DOCUMENTATION
          README, Markdown, JavaDoc, comments, documentation,
          guides, API documentation and docs.

        Rules:

        1. Every file must belong to exactly one category.
        2. Use the patch as the primary evidence.
        3. If no patch is available, classify the file using its path and filename.
        4. Return ONLY valid JSON.
        5. Do not wrap the JSON inside markdown.
        6. Do not explain your reasoning.

        Response format:

        {
          "classifications": [
            {
              "filePath": "...",
              "category": "SECURITY"
            }
          ]
        }
        """;

    StringJoiner changedFiles = new StringJoiner("\n\n");

    for (ChangedFile file : state.changedFiles()) {
      String patch = file.getPatch();
      if (patch == null || patch.isBlank()) {
        patch = """
            No textual diff is available.

            GitHub omitted the patch because:
              - the file is binary, or
              - the diff is too large.

            Classify this file using its path and filename.
                """;
      }
      changedFiles.add("""
          =======================================

          File:
          %s

          Patch:
          %s
          """.formatted(
          file.getFilePath(),
          patch));
    }

    String userPrompt = """
        Review Run ID: %d

        Pull Request Number: %d

        Changed Files:

        %s
        """.formatted(
        state.reviewRunId(),
        state.pullRequestNumber(),
        changedFiles);

    LlmRequest request = new LlmRequest();
    request.setSystemPrompt(systemPrompt);
    request.setUserPrompt(userPrompt);
    request.setTemperature(0.0);

    return request;
  }
}
