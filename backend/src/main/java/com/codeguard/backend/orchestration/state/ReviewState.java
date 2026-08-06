package com.codeguard.backend.orchestration.state;

import java.util.List;
import java.util.Map;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import com.codeguard.backend.orchestration.model.ChangedFile;
import com.codeguard.backend.orchestration.model.FileClassification;

public class ReviewState extends AgentState {
    public static final String REVIEW_RUN_ID = "reviewRunId";
    public static final String REPO_ID = "repoId";
    public static final String PULL_REQUEST_NUMBER = "pullRequestNumber";
    public static final String CHANGED_FILES = "changedFiles";
    public static final String CLASSIFICATIONS = "classifications";
    public static final String STATUS = "status";
    public static final String FAILURE_REASON = "failureReason";

    public static final Map<String, Channel<?>> SCHEMA = Map.of(
            REVIEW_RUN_ID, Channels.<Long>base(() -> 0L),
            REPO_ID, Channels.<Long>base(() -> 0L),
            PULL_REQUEST_NUMBER, Channels.<Long>base(() -> 0L),
            CHANGED_FILES, Channels.base(() -> List.<ChangedFile>of()),
            CLASSIFICATIONS, Channels.base(() -> List.<FileClassification>of()),
            STATUS, Channels.base(() -> ClassificationStatus.PENDING),
            FAILURE_REASON, Channels.<String>base(() -> ""));

    public enum ClassificationStatus {
        PENDING,
        COMPLETED,
        FAILED
    }

    public ReviewState(Map<String, Object> init) {
        super(init);
    }

    public Long reviewRunId() {
        return this.<Long>value("reviewRunId")
                .orElse(null);
    }

    public Long repoId() {
        return this.<Long>value("repoId")
                .orElse(null);
    }

    public Long pullRequestNumber() {
        return this.<Long>value("pullRequestNumber")
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    public List<ChangedFile> changedFiles() {
        return (List<ChangedFile>) value("changedFiles")
                .orElse(List.of());
    }

    @SuppressWarnings("unchecked")
    public List<FileClassification> classifications() {
        return (List<FileClassification>) value("classifications")
                .orElse(List.of());
    }

    public ClassificationStatus status() {
        return value("status")
                .map(ClassificationStatus.class::cast)
                .orElse(ClassificationStatus.PENDING);
    }

    public String failureReason() {
        return value("failureReason")
                .map(String.class::cast)
                .orElse(null);
    }

}