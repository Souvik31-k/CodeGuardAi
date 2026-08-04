package com.codeguard.backend.llm.groq.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroqChatResponse {
    private String id;
    private String object;
    private Long created;
    private String model;
    private List<GroqChoice> choices;
    private GroqUsage usage;
}
