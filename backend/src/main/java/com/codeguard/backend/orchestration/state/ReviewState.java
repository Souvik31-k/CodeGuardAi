package com.codeguard.backend.orchestration.state;

import java.util.List;

import com.codeguard.backend.orchestration.model.ChangedFile;
import com.codeguard.backend.orchestration.model.FileClassification;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewState {

    public enum ClassificationStatus {
        PENDING,
        COMPLETED,
        FAILED
    }

    private Long reviewRunId;
    private Long repoId;
    private Long pullRequestNumber;
    private List<ChangedFile> changedFiled;
    private List<FileClassification> classifications;
    private ClassificationStatus status;
    private String failureReason;

}