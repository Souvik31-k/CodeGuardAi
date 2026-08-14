package com.codeguard.backend.orchestration.state;

import java.util.List;
import java.util.Map;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import com.codeguard.backend.orchestration.model.ChangedFile;
import com.codeguard.backend.orchestration.model.FileClassification;
import com.codeguard.backend.orchestration.model.ReviewResult;
import com.codeguard.backend.orchestration.model.SpecialistResult;

public class ReviewState extends AgentState {

    public static final String REVIEW_RUN_ID = "reviewRunId";
    public static final String REPO_ID = "repoId";
    public static final String PULL_REQUEST_NUMBER = "pullRequestNumber";

    public static final String CHANGED_FILES = "changedFiles";
    public static final String CLASSIFICATIONS = "classifications";

    public static final String STATUS = "status";
    public static final String FAILURE_REASON = "failureReason";

    public static final String DOCUMENTATION_RESULT = "documentationResult";
    public static final String TEST_RESULT = "testResult";
    public static final String SECURITY_RESULT = "securiytResult";
    public static final String QUALITY_RESULT = "qualityResult";

    public static final String AGGREGATED_RESULT = "aggregatedResult";

    public static final Map<String, Channel<?>> SCHEMA = Map.ofEntries(
            Map.entry(REVIEW_RUN_ID, Channels.<Long>base(() -> 0L)),

            Map.entry(REPO_ID, Channels.<Long>base(() -> 0L)),

            Map.entry(PULL_REQUEST_NUMBER, Channels.<Long>base(() -> 0L)),

            Map.entry(CHANGED_FILES, Channels.<List<ChangedFile>>base(() -> List.<ChangedFile>of())),

            Map.entry(CLASSIFICATIONS, Channels.<List<FileClassification>>base(() -> List.<FileClassification>of())),

            Map.entry(STATUS, Channels.<ClassificationStatus>base(() -> ClassificationStatus.PENDING)),

            Map.entry(FAILURE_REASON, Channels.<String>base(() -> "")),

            Map.entry(DOCUMENTATION_RESULT, Channels.<SpecialistResult>base(() -> null)),

            Map.entry(SECURITY_RESULT, Channels.<SpecialistResult>base(() -> null)),

            Map.entry(TEST_RESULT, Channels.<SpecialistResult>base(() -> null)),

            Map.entry(QUALITY_RESULT, Channels.<SpecialistResult>base(() -> null)),

            Map.entry(AGGREGATED_RESULT, Channels.<ReviewResult>base(() -> null)));

    public enum ClassificationStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        PARTIALLY_COMPLETED,
        FAILED
    }

    public ReviewState(Map<String, Object> init) {
        super(init);
    }

    public Long reviewRunId() {
        return this.<Long>value(REVIEW_RUN_ID)
                .orElse(null);
    }

    public Long repoId() {
        return this.<Long>value(REPO_ID)
                .orElse(null);
    }

    public Long pullRequestNumber() {
        return this.<Long>value(PULL_REQUEST_NUMBER)
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    public List<ChangedFile> changedFiles() {
        return (List<ChangedFile>) value(CHANGED_FILES)
                .orElse(List.of());
    }

    @SuppressWarnings("unchecked")
    public List<FileClassification> classifications() {
        return (List<FileClassification>) value(CLASSIFICATIONS)
                .orElse(List.of());
    }

    public ClassificationStatus status() {
        return value(STATUS)
                .map(ClassificationStatus.class::cast)
                .orElse(ClassificationStatus.PENDING);
    }

    public String failureReason() {
        return value(FAILURE_REASON)
                .map(String.class::cast)
                .orElse(null);
    }

    public SpecialistResult documentationResult() {
        return this.<SpecialistResult>value(DOCUMENTATION_RESULT)
                .orElse(null);
    }

    public SpecialistResult testResult() {
        return this.<SpecialistResult>value(TEST_RESULT)
                .orElse(null);
    }

    public SpecialistResult securityResult() {
        return this.<SpecialistResult>value(SECURITY_RESULT)
                .orElse(null);
    }

    public SpecialistResult qualityResult() {
        return this.<SpecialistResult>value(QUALITY_RESULT)
                .orElse(null);
    }

    public ReviewResult aggregatedResult() {
        return this.<ReviewResult>value(AGGREGATED_RESULT)
                .orElse(null);
    }

}