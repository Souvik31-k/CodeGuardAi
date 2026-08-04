package com.codeguard.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codeguard.backend.llm.LlmProvider;
import com.codeguard.backend.llm.LlmRequest;
import com.codeguard.backend.llm.LlmResponse;

@RestController
public class GroqController {

    public final LlmProvider llmProvider;

    public GroqController(LlmProvider llmProvider) {
        this.llmProvider = llmProvider;
    }

    @GetMapping("/test")
    public LlmResponse test() {
        LlmRequest request = new LlmRequest();
        request.setSystemPrompt("You are a helpful assistant.");
        request.setUserPrompt("Hello my Coding assitant How are you");
        request.setTemperature(0.5);

        LlmResponse response = llmProvider.generate(request);

        return response;
    }
}
