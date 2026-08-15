package com.codeguard.backend.orchestration.prompt;

import java.util.List;
import java.util.StringJoiner;

import org.springframework.stereotype.Component;

import com.codeguard.backend.llm.LlmRequest;
import com.codeguard.backend.orchestration.model.ChangedFile;
import com.codeguard.backend.orchestration.state.ReviewState;

@Component
public class QualityPromptBuilder {

    public LlmRequest buildPrompt(ReviewState state, List<ChangedFile> qualityFiles) {

        String systemPrompt = """
                You are the Quality Agent of the CodeGuard AI Platform.

                Analyze the provided code changes for code-quality and
                maintainability issues.

                Look specifically for:

                - Code duplication
                - Excessive code complexity
                - Poor or unclear naming
                - Poor readability
                - Violation of clean-code principles
                - Poor separation of responsibilities
                - Large or overly complex methods
                - Unnecessary code or redundant logic
                - Poor error-handling structure
                - Maintainability problems
                - Violations of the provided coding standards

                Base your findings primarily on the actual code changes
                contained in the patches.

                If coding standards are provided, use them as the primary
                reference when determining whether a quality issue violates
                the repository's standards.

                Do not report:

                - Security vulnerabilities
                - Test failures or missing test coverage
                - Documentation issues
                - Performance issues

                Return ONLY valid JSON.

                Response format:
                {
                  "findings": [
                    {
                      "agentType": "QUALITY",
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

                If no quality problems are found, return:

                {
                  "findings": []
                }

                Allowed severity values:
                CRITICAL, HIGH, MEDIUM, LOW

                Every finding MUST contain a severity.

                Do not wrap the JSON in markdown.
                Do not include explanations outside the JSON.
                """;

        StringJoiner files = new StringJoiner("\n\n");

        for (ChangedFile file : qualityFiles) {
            files.add("""
                    File Path:
                    %s

                    File Patch:
                    %s
                    """.formatted(
                    file.getFilePath(),
                    file.getPatch() == null ? "(No file patch available)" : file.getPatch()));
        }

        String userPrompt = """
                Review Run Id: %d

                Pull Request Number: %d

                Changed Files: %s
                """.formatted(
                state.reviewRunId(),
                state.pullRequestNumber(),
                files);

        return new LlmRequest(
                systemPrompt,
                userPrompt,
                0.0);
    }
}
