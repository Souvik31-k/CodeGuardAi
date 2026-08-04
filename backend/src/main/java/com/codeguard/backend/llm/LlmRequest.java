package com.codeguard.backend.llm;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LlmRequest {

    // define the behaviour of the llm
    private String systemPrompt;

    // define the user prompt
    private String userPrompt;

    // Randomness of the llm response
    private Double temperature;

}
