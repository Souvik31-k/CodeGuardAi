package com.codeguard.backend.orchestration.prompt;

import java.util.List;
import java.util.StringJoiner;

import org.springframework.stereotype.Component;

import com.codeguard.backend.llm.LlmRequest;
import com.codeguard.backend.orchestration.model.ChangedFile;
import com.codeguard.backend.orchestration.state.ReviewState;

@Component
public class DocumentPromptBuilder {

  public LlmRequest buildPrompt(ReviewState state, List<ChangedFile> documentFiles) {
    String systemPrompt = """
        You are the Documentation Specialist of the CodeGuard AI Platform.

        Analyze ONLY the provided documentation-related changes.

        Identify issues such as:
        - Missing or outdated documentation
        - Incorrect README or API documentation
        - Inconsistent documentation with the changed code
        - Missing JavaDoc for important public APIs
        - Incorrect setup or usage instructions
        - Broken or incomplete examples
        - Documentation that contradicts the implementation

        Base findings primarily on the provided patch.
        Do not invent information that is not present in the patch.

        Return ONLY valid JSON.
        Do not use Markdown code fences.
        Do not include explanations outside the JSON.

        Response format:
        {
          "findings": [
            {
              "agentType": DOCUMENTATION,
              "severity": "LOW",
              "title": "Short description",
              "filePath": "path/to/file",
              "lineNumber": 10,
              "details": {
                "rule": "DOCUMENTATION",
                "evidence": "What is wrong",
                "recommendation": "How to improve it"
              }
            }
          ]
        }

        If no documentation issues are found, return:

        {
          "findings": []
        }

        Allowed severity values:
        CRITICAL, HIGH, MEDIUM, LOW

        Do not wrap the JSON in markdown.
        Do not include explanations outside the JSON.
        """;
    ;

    StringJoiner files = new StringJoiner("\n\n");

    for (ChangedFile file : documentFiles) {
      files.add("""
          =======================================


          File:
          %s


          Patch:
          %s
          """.formatted(
          file.getFilePath(),
          file.getPatch() == null
              ? "(No File Patch Available)"
              : file.getPatch()));
    }

    String userPrompt = """
        Review Run Id:
        %n

        Pull Request Number:
        %n

        Documentation-Related Changed Files:
        """.formatted(
        state.reviewRunId(),
        state.pullRequestNumber(),
        files);

    LlmRequest request = new LlmRequest(
        systemPrompt, userPrompt, 0.0);

    return request;

  }
}
