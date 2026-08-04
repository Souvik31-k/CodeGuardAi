package com.codeguard.backend.llm;

public interface LlmProvider {
    LlmResponse generate(LlmRequest request);

}