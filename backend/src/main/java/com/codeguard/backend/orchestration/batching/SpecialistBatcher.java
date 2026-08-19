package com.codeguard.backend.orchestration.batching;

import java.util.ArrayList;
import java.util.List;

import com.codeguard.backend.orchestration.model.ChangedFile;

public class SpecialistBatcher {

    private SpecialistBatcher() {
    }

    public static List<List<ChangedFile>> batch(
            List<ChangedFile> files,
            int maxChars) {

        List<List<ChangedFile>> batches = new ArrayList<>();

        List<ChangedFile> currentBatch = new ArrayList<>();

        int currentChars = 0;

        for (ChangedFile file : files) {

            int fileChars = estimateFileChars(file);

            /*
             * If adding this file would exceed the batch limit,
             * finish the current batch first.
             */
            if (!currentBatch.isEmpty()
                    && currentChars + fileChars > maxChars) {

                batches.add(currentBatch);

                currentBatch = new ArrayList<>();
                currentChars = 0;
            }

            currentBatch.add(file);
            currentChars += fileChars;
        }

        if (!currentBatch.isEmpty()) {

            batches.add(currentBatch);

        }

        return batches;

    }

    private static int estimateFileChars(ChangedFile file) {

        String path = file.getFilePath() == null
                ? ""
                : file.getFilePath();

        String patch = file.getPatch() == null
                ? ""
                : file.getPatch();

        return path.length() + patch.length();
    }
}
