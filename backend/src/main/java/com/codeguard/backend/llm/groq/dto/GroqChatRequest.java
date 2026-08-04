package com.codeguard.backend.llm.groq.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GroqChatRequest {

    private String model;
    private List<GroqMessage> messages;
    private Double temperature;
}
