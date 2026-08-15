package com.codeguard.backend.orchestration.model;

import java.io.Serializable;
import java.util.List;

import com.codeguard.backend.enums.AgentType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SpecialistResult implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum SpecialistStatus {
        NOT_EXECUTED,
        COMPLETED,
        FAILED,
        TIMEOUT
    }

    private AgentType agentType;
    private SpecialistStatus status;
    private List<AgentFinding> findings;
    private String failureReason;

    public static SpecialistResult notExecuted(AgentType agentType) {
        return new SpecialistResult(
                agentType,
                SpecialistStatus.NOT_EXECUTED,
                List.of(),
                null);
    }
}
