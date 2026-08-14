package com.codeguard.backend.llm.groq;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.codeguard.backend.exception.LlmProviderException;
import com.codeguard.backend.llm.LlmProvider;
import com.codeguard.backend.llm.LlmRequest;
import com.codeguard.backend.llm.LlmResponse;
import com.codeguard.backend.llm.groq.dto.GroqChatRequest;
import com.codeguard.backend.llm.groq.dto.GroqChatResponse;
import com.codeguard.backend.llm.groq.mapper.GroqMapper;

@Component
public class GroqLlmProvider implements LlmProvider {

    private final GroqMapper mapper;
    private final GroqProperties properties;
    private final RestClient restClient;

    public GroqLlmProvider(GroqMapper mapper, GroqProperties properties,
            @Qualifier("groqRestClient") RestClient restClient) {
        this.mapper = mapper;
        this.properties = properties;
        this.restClient = restClient;
    }

    /**
     * Takes the genric LLM Request
     * Converts it into the Groq Specific Request
     * Sends over to the groq to capture the response
     * converts the response into Llm generic response
     */
    @Override
    public LlmResponse generate(LlmRequest request) {
        GroqChatRequest chatRequest = mapper.convertRequest(request, properties.getModel());
        GroqChatResponse response = null;
        try {
            response = restClient.post()
                    .uri(properties.getBaseUrl())
                    .header(HttpHeaders.AUTHORIZATION,
                            "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(chatRequest)
                    .retrieve()
                    .body(GroqChatResponse.class);
        } catch (RestClientException e) {
            throw new LlmProviderException(
                    "Failed to call Groq API.",
                    e);
        }

        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new LlmProviderException(
                    "Failed to call Groq API.");
        }

        return mapper.convertResponse(response);
    }

}
