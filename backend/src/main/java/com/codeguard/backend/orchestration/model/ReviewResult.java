package com.codeguard.backend.orchestration.model;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResult implements Serializable {

    private static final long serialVersionUID = 1L;
    private List<AgentFinding> findings;
    private List<SpecialistResult> specialistResult;
    private ReviewStatus reviewStatus;
    private String summary;

    public enum ReviewStatus {
        COMPLETED,
        PARTIALLY_COMPLETED,
        FAILED
    }

    public static ReviewResult empty() {
        return new ReviewResult(
                List.of(),
                List.of(),
                null,
                null);
    }
}
