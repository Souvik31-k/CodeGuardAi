package com.codeguard.backend.llm.groq;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.codeguard.backend.exception.LlmProviderException;
import com.codeguard.backend.llm.LlmProvider;
import com.codeguard.backend.llm.LlmRequest;
import com.codeguard.backend.llm.LlmResponse;
import com.codeguard.backend.llm.groq.dto.GroqChatRequest;
import com.codeguard.backend.llm.groq.dto.GroqChatResponse;
import com.codeguard.backend.llm.groq.mapper.GroqMapper;

@Component
public class GroqLlmProvider implements LlmProvider {

    private static final double TOKEN_SAFETY_FACTOR = 1.15;

    private final GroqMapper mapper;
    private final GroqProperties properties;
    private final RestClient restClient;
    private final GroqRateLimiter limiter;

    public GroqLlmProvider(GroqMapper mapper, GroqProperties properties,
            @Qualifier("groqRestClient") RestClient restClient, GroqRateLimiter limiter) {
        this.mapper = mapper;
        this.properties = properties;
        this.restClient = restClient;
        this.limiter = limiter;
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

        long estimateToken = estimateToken(chatRequest);

        limiter.acquire(estimateToken);

        int max_attempt = 3;

        for (int attempt = 1; attempt <= max_attempt; attempt++) {

            try {
                ResponseEntity<GroqChatResponse> responseEntity = restClient.post()
                        .uri(properties.getBaseUrl())
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + properties.getApiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(chatRequest)
                        .retrieve()
                        .toEntity(GroqChatResponse.class);

                updateRateLimitState(responseEntity.getHeaders());

                GroqChatResponse response = responseEntity.getBody();

                if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                    throw new LlmProviderException(
                            "Failed to call Groq API.");
                }

                return mapper.convertResponse(response);

            } catch (HttpClientErrorException.TooManyRequests e) {

                if (attempt == max_attempt) {
                    throw new LlmProviderException(
                            "Failed to call Groq API.",
                            e);
                }

                Duration retryAfter = extractRetryAfter(e.getResponseHeaders());

                limiter.blockFor(retryAfter);

                limiter.awaitAvailability();
                ;

            }
        }
        throw new LlmProviderException("Failed to call Groq API.");
    }

    private long estimateToken(GroqChatRequest request) {

        int characters = request.getMessages()
                .stream()
                .map((GroqMessage) -> GroqMessage.getContent())
                .filter((content) -> content != null)
                .mapToInt((content) -> content.length())
                .sum();

        long estimateTokens = Math.max(1, (characters + 3) / 4);

        return (long) Math.ceil(estimateTokens * TOKEN_SAFETY_FACTOR);
    }

    private void updateRateLimitState(HttpHeaders headers) {

        String remaining = headers.getFirst("x-ratelimit-remaining-tokens");

        String reset = headers.getFirst("x-ratelimit-reset-tokens");

        if (remaining == null || reset == null) {
            return;
        }

        try {
            long remainingTokens = Long.parseLong(remaining);

            Duration resetDuration = parseResetDuration(reset);

            limiter.update(remainingTokens, resetDuration);
        } catch (NumberFormatException e) {

            // Don't break a successful LLM request
            // because rate-limit metadata couldn't be parsed.

        }
    }

    private Duration parseResetDuration(String reset) {

        String normalized = reset.trim().toLowerCase();

        if (normalized.endsWith("ms")) {

            long millis = Long.parseLong(
                    normalized.substring(
                            0,
                            normalized.length() - 2));

            return Duration.ofMillis(millis);
        }

        if (normalized.endsWith("s")) {
            double seconds = Double.parseDouble(
                    normalized.substring(
                            0,
                            normalized.length() - 1));

            return Duration.ofMillis(
                    (long) (seconds * 1000));
        }

        return Duration.ofSeconds(Long.parseLong(normalized));

    }

    private Duration extractRetryAfter(HttpHeaders headers) {

        String retryAfter = headers.getFirst("retry-after");

        if (retryAfter == null) {
            return Duration.ofSeconds(5);
        }

        return Duration.ofSeconds(Long.parseLong(retryAfter));
    }

}
