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
