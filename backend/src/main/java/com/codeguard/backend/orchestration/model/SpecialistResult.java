package com.codeguard.backend.orchestration.model;

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
public class SpecialistResult {

    public enum SpecialistStatus {
        COMPLETED,
        FAILED,
        TIMEOUT
    }

    private AgentType agentType;
    private SpecialistStatus status;
    private List<AgentFinding> findings;
    private String failureReason;
}
