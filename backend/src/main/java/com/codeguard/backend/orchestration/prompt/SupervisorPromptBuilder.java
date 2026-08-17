package com.codeguard.backend.orchestration.prompt;

import java.util.StringJoiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.codeguard.backend.llm.LlmRequest;
import com.codeguard.backend.orchestration.model.ChangedFile;
import com.codeguard.backend.orchestration.state.ReviewState;

@Component
public class SupervisorPromptBuilder {

  private static final int MAX_PATCH_CHARS_PER_FILE = 3000;
  private static final int MAX_TOTAL_PATCH_CHARS = 30000;
  private static final Logger log = LoggerFactory.getLogger(SupervisorPromptBuilder.class);

  public LlmRequest buildPrompt(ReviewState state) {
    String systemPrompt = """
        You are the Supervisor Agent of the CodeGuard AI Platform.

        Your job is to classify changed files in a pull request
        into one or more review categories.

        Use these categories:

        - SECURITY
          Authentication, authorization, credentials, secrets,
          cryptography, JWT, OAuth, input validation, SQL injection,
          XSS, CSRF, and other security-sensitive code.

        - QUALITY
          General application logic, architecture, maintainability,
          error handling, code smells, performance, and design issues.

        - TEST
          Unit tests, integration tests, test configuration,
          test utilities, and test-related code

        - DOCUMENTATION
          README files, documentation, comments, markdown,
          API documentation, and other documentation files.

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
    int totalChars = 0;
    for (ChangedFile file : state.changedFiles()) {
      String patch = file.getPatch();
      if (patch == null || patch.isBlank()) {
        patch = "(No file patch available)";
      }

      int remaining = MAX_TOTAL_PATCH_CHARS - totalChars;

      if (remaining <= 0) {
        changedFiles.add("""
            Additional changed files exist but their patches
            were omitted because the Supervisor input budget
            was reached.
            """);
        break;
      }

      int maxChars = Math.min(MAX_PATCH_CHARS_PER_FILE, remaining);

      if (patch.length() > maxChars) {

        patch = patch.substring(0, maxChars)
            + "\n[PATCH TRUNCATED]";
      }

      changedFiles.add("""
          File:
          %s

          Patch:
          %s
          """.formatted(
          file.getFilePath(),
          patch));

      totalChars += patch.length();
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

    log.info(
        "Supervisor prompt size: {} characters for {} changed files",
        userPrompt.length(),
        changedFiles.length());

    return request;
  }
}
