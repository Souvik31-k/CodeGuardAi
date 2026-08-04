package com.codeguard.backend.llm.groq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GroqChoice {
    private Integer index;
    private GroqMessage message;

    @JsonProperty("finish_reason")
    private String finishReason;
}
