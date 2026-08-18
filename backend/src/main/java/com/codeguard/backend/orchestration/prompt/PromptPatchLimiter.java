package com.codeguard.backend.orchestration.prompt;

import java.util.List;
import java.util.StringJoiner;

import com.codeguard.backend.orchestration.model.ChangedFile;

public final class PromptPatchLimiter {
    private static final int MAX_PATCH_CHARS_PER_FILE = 2000;
    private static final int TOTAL_MAX_PATCH_CHARS = 10000;

    private PromptPatchLimiter() {
    }

    public static String buildChangedFiles(List<ChangedFile> files) {
        StringJoiner changedFiles = new StringJoiner("\n\n");

        int totalChars = 0;

        for (ChangedFile file : files) {

            String patch = file.getPatch();

            if (patch == null || patch.isBlank()) {

                patch = "(No patch available)";

            }

            if (totalChars >= MAX_PATCH_CHARS_PER_FILE) {

                changedFiles.add("[Additional changed files omitted due to prompt size limit]");

                break;
            }

            int remainingChars = TOTAL_MAX_PATCH_CHARS - totalChars;

            int allowedChars = Math.min(MAX_PATCH_CHARS_PER_FILE, remainingChars);

            boolean truncated = patch.length() > allowedChars;

            String bounded = truncated
                    ? patch.substring(0, allowedChars)
                            + "\n[PATCH TRUNCATED]"
                    : patch;

            changedFiles.add("""
                    File path:
                    %s

                    File patch:
                    %s
                    """.formatted(
                    file.getFilePath(),
                    bounded));
            totalChars += bounded.length();

        }

        return changedFiles.toString();
    }
}
