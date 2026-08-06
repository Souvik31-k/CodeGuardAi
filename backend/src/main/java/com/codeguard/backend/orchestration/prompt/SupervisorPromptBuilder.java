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

        Your job is to classify each changed file into exactly one specialist agent.

        Classify based on the ACTUAL CODE CHANGES shown in the patch, not only the filename.

        Use these categories:

        - SECURITY
          Authentication, authorization, encryption, secrets, JWT, passwords,
          input validation, SQL injection, XSS, CSRF, permissions, OAuth, security configuration.

        - PERFORMANCE
          Algorithms, loops, caching, database queries, memory usage,
          concurrency, threading, asynchronous execution, collections,
          CPU-intensive operations and performance optimizations.

        - TEST
          Unit tests, integration tests, mocks, assertions, JUnit,
          Mockito, TestNG, testing utilities.

        - DOCUMENTATION
          README, Markdown, JavaDoc, comments, documentation,
          guides, API documentation and docs.

        Rules:

        1. Every file must belong to exactly one category.
        2. Use the patch as the primary evidence.
        3. Use the filename only if the patch is insufficient.
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
      changedFiles.add("""
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
