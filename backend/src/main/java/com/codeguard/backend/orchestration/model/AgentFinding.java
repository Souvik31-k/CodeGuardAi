package com.codeguard.backend.orchestration.model;

import com.codeguard.backend.model.Findings.Severity;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgentFinding {

    private String filePath;
    private Severity severitiy;
    private String title;
    private Integer lineNumber;
    private JsonNode details;
}
