package com.codeguard.backend.llm.groq.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.codeguard.backend.llm.LlmRequest;
import com.codeguard.backend.llm.LlmResponse;
import com.codeguard.backend.llm.groq.dto.GroqChatRequest;
import com.codeguard.backend.llm.groq.dto.GroqChatResponse;
import com.codeguard.backend.llm.groq.dto.GroqChoice;
import com.codeguard.backend.llm.groq.dto.GroqMessage;

@Component
public class GroqMapper {

    /**
     * convert the llm request to groq chat request dto(format)
     */
    public GroqChatRequest convertRequest(LlmRequest request, String model) {
        GroqChatRequest chatRequest = new GroqChatRequest();
        chatRequest.setModel(model);
        chatRequest.setTemperature(request.getTemperature());
        chatRequest.setMessages(List.of(
                systemMessage(request.getSystemPrompt()),
                userMessage(request.getUserPrompt())));
        return chatRequest;
    }

    /**
     * convert Groq's response to generic llm response
     */

    public LlmResponse convertResponse(GroqChatResponse response) {
        if (response == null) {
            throw new IllegalStateException("Groq returned empty response");
        }

        if (response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new IllegalStateException("Groq returned no choices");
        }

        GroqChoice choice = response.getChoices().getFirst();
        LlmResponse llmResponse = new LlmResponse();
        llmResponse.setContent(choice.getMessage().getContent());
        llmResponse.setModel(response.getModel());

        if (response.getUsage() != null) {
            llmResponse.setPromptTokens(response.getUsage().getPromptTokens());
            llmResponse.setCompletionTokens(response.getUsage().getCompletionTokens());
        }
        return llmResponse;
    }

    private GroqMessage systemMessage(String prompt) {
        return new GroqMessage("system", prompt);
    }

    private GroqMessage userMessage(String prompt) {
        return new GroqMessage("user", prompt);
    }

}
