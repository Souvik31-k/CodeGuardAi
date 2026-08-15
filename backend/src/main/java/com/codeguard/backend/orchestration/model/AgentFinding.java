package com.codeguard.backend.orchestration.model;

import java.io.Serializable;

import com.codeguard.backend.enums.AgentType;
import com.codeguard.backend.enums.Severity;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgentFinding implements Serializable {

    private static final long serialVersionUID = 1L;
    private AgentType agentType;
    private String filePath;
    private Severity severity;
    private String title;
    private Integer lineNumber;
    private JsonNode details;
}
