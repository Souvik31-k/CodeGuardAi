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

  private static final int MAX_PATCH_CHARS_PER_FILE = 1500;
  private static final int MAX_TOTAL_PATCH_CHARS = 12000;
  private static final Logger log = LoggerFactory.getLogger(SupervisorPromptBuilder.class);

  public LlmRequest buildPrompt(ReviewState state) {
    String systemPrompt = """
        You are the Supervisor Agent of the CodeGuard AI Platform.

        Your job is to classify changed files in a pull request
        into one or more review categories.

        Use these categories:

        - SECURITY
          Code whose primary purpose or changed behavior involves
          authentication, authorization, credentials, secrets,
          cryptography, JWT, OAuth, security configuration,
          input validation for security purposes, SQL injection,
          XSS, CSRF, or other security-sensitive functionality.

          Do NOT classify files as SECURITY merely because they mention
          security concepts, contain security-related comments, or invoke
          security components. Classify based on the primary purpose of
          the changed code.

        - QUALITY
          General application or infrastructure code whose primary purpose
          involves business logic, application logic, architecture,
          maintainability, error handling, code smells, performance,
          readability, design, or clean-code practices.

          QUALITY is the default category only when the file does not
          primarily belong to SECURITY, TEST, or DOCUMENTATION.

          Do NOT classify a file as QUALITY merely because it supports
          another review category. Classify based on the primary purpose
          of the changed code.

        - TEST
          Actual test code and test infrastructure:
          unit tests, integration tests, test fixtures,
          test utilities, test configuration, mocks,
          and test resources.

          Do NOT classify application components that merely
          analyze, execute, orchestrate, or generate tests as TEST.
          Classify based on the primary purpose of the changed code.

        - DOCUMENTATION
          Files whose primary purpose is communicating information
          about the software rather than executing application logic.

          This includes README files, Markdown documentation,
          API documentation, documentation pages, usage guides,
          setup instructions, examples intended primarily for users,
          and documentation-specific comments or configuration.

          Do NOT classify source-code files as DOCUMENTATION merely
          because they contain comments, JavaDoc, or documentation
          strings. Classify the file according to the primary purpose
          of the changed content.

        Rules:

        1. Every file must belong to exactly one category.
        2. Use the available patch as the primary evidence.
        3. If the patch is truncated, use the available portion of the patch together
           with the file path and filename.
        4. If no patch is available, classify the file using its path and filename.
        5. Do not invent code or behavior that is not present in the provided evidence.
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

    int totalPatchChars = 0;
    int truncatedFiles = 0;

    for (ChangedFile file : state.changedFiles()) {

      String patch = file.getPatch();

      if (patch == null || patch.isBlank()) {

        changedFiles.add("""
            File:
            %s

            Patch:
            (No file patch available)

            Classification guidance:
            Classify using the file path and filename.
            Do not invent code that is not available.
            """.formatted(file.getFilePath()));

        continue;
      }

      int remaining = MAX_TOTAL_PATCH_CHARS - totalPatchChars;

      String boundedPatch;

      if (remaining <= 0) {

        boundedPatch = """
            (Patch omitted because the Supervisor patch
            budget was reached. Classify using the file
            path and filename.)
            """;

      } else {

        int maxChars = Math.min(
            MAX_PATCH_CHARS_PER_FILE,
            remaining);

        if (patch.length() > maxChars) {

          boundedPatch = patch.substring(0, maxChars)
              + "\n[PATCH TRUNCATED]";

          totalPatchChars += maxChars;
          truncatedFiles++;

        } else {

          boundedPatch = patch;

          totalPatchChars += patch.length();
        }
      }

      changedFiles.add("""
          File:
          %s

          Patch:
          %s
          """.formatted(
          file.getFilePath(),
          boundedPatch));
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
        "Supervisor prompt: {} chars, patch chars: {}, files: {}, truncated: {}",
        userPrompt.length(),
        totalPatchChars,
        state.changedFiles().size(),
        truncatedFiles);

    return request;
  }
}
