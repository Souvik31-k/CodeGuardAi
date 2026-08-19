package com.codeguard.backend.orchestration.dto;

import java.util.List;

import com.codeguard.backend.orchestration.model.AgentFinding;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SpecialistAnalysisResponse {

    private List<AgentFinding> findings;
}
