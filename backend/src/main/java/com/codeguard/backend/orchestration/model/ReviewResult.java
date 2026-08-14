package com.codeguard.backend.orchestration.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResult {

    private List<AgentFinding> findings;
    private List<SpecialistResult> specialistResult;
    private ReviewStatus reviewStatus;
    private String summary;

    public enum ReviewStatus {
        COMPLETED,
        PARTIALLY_COMPLETED,
        FAILED
    }
}
