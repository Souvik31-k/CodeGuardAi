package com.codeguard.backend.orchestration.prompt;

import java.util.List;

import org.springframework.stereotype.Component;

import com.codeguard.backend.llm.LlmRequest;
import com.codeguard.backend.orchestration.model.ChangedFile;
import com.codeguard.backend.orchestration.state.ReviewState;

@Component
public class TestPromptBuilder {

  public LlmRequest buildPrompt(ReviewState state, List<ChangedFile> testFiles) {
    String systemPrompt = """
        You are the Test Specialist of the CodeGuard AI Platform.

        Analyze ONLY the provided test-related code changes.

        Your job is to identify defects, weaknesses, or missing coverage in tests.

        Check for:

        - Missing test coverage for newly added or modified behavior
        - Incorrect or incomplete assertions
        - Tests that do not actually verify the intended behavior
        - Missing edge-case tests
        - Missing negative/error-path tests
        - Weak or overly broad assertions
        - Incorrect test setup or teardown
        - Incorrect mocking or verification
        - Tests that may produce false positives or false negatives
        - Flaky or non-deterministic tests
        - Duplicated or unnecessary test cases
        - Integration tests that fail to verify important interactions

        Base findings primarily on the provided patch.
        Do not invent behavior that cannot be inferred from the provided changes.

        For each finding, provide:
        - severity
        - short title
        - file path
        - line number when identifiable
        - evidence explaining the problem
        - recommendation for improvement

        Severity definitions:

        CRITICAL:
        A severe testing deficiency that can allow major defects or security issues
        to pass undetected.

        HIGH:
        A significant missing or incorrect test that should be addressed before
        the changed functionality is considered reliable.

        MEDIUM:
        A meaningful testing weakness that reduces confidence in the implementation.

        LOW:
        A minor testing improvement or maintainability issue.

        Response format:

        {
          "findings": [
            {
              "agentType": "TEST",
              "severity": "MEDIUM",
              "title": "Missing edge-case test",
              "filePath": "src/test/java/example/UserServiceTest.java",
              "lineNumber": 45,
              "details": {
                "rule": "MISSING_EDGE_CASE",
                "evidence": "The test verifies only the successful input path.",
                "recommendation": "Add a test covering invalid or boundary input."
              }
            }
          ]
        }

        If no test issues are found, return:

        {
          "findings": []
        }

        Allowed severity values:
        CRITICAL, HIGH, MEDIUM, LOW
        """;

    String files = PromptPatchLimiter.buildChangedFiles(testFiles);

    String userPrompt = """
        Review Run Id: %d

         Pull Request Number: %d

        Test Changes: %s

        """.formatted(
        state.reviewRunId(),
        state.pullRequestNumber(),
        files);

    return new LlmRequest(systemPrompt, userPrompt, 0.0);
  }
}
