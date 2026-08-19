package com.codeguard.backend.llm.groq;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;

@Component
public class GroqRateLimiter {
    private static final long DEFAULT_LIMIT = 8000;

    private long remainingTokens = DEFAULT_LIMIT;
    private Instant resetAt = Instant.now();

    public synchronized void acquire(long estimateTokens) {

        while (remainingTokens < estimateTokens) {

            waitForReset();
            refreshWindowIfExpired();
        }

        remainingTokens -= estimateTokens;
    }

    public synchronized void update(
            long remainingTokens,
            Duration resetDuration) {

        this.remainingTokens = remainingTokens;

        this.resetAt = Instant.now()
                .plus(resetDuration);

        notifyAll();
    }

    public synchronized void blockFor(Duration duration) {

        Instant newResetAt = Instant.now().plus(duration);

        if (newResetAt.isAfter(resetAt)) {
            resetAt = newResetAt;
        }

        remainingTokens = 0;

        notifyAll();
    }

    public synchronized void awaitAvailability() {

        while (Instant.now().isBefore(resetAt)) {

            long waitMillis = Duration
                    .between(Instant.now(), resetAt)
                    .toMillis();

            if (waitMillis <= 0) {
                break;
            }

            try {
                wait(waitMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();

                throw new IllegalStateException(
                        "Interrupted while waiting for Groq rate limit.",
                        e);
            }
        }
    }

    private void waitForReset() {

        long waitMillis = Duration
                .between(Instant.now(), resetAt)
                .toMillis();

        if (waitMillis <= 0) {
            return;
        }

        try {

            wait(waitMillis);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Interrupted while waiting for Groq token capacity",
                    e);
        }
    }

    private void refreshWindowIfExpired() {

        if (Instant.now().isAfter(resetAt)) {
            remainingTokens = DEFAULT_LIMIT;
            resetAt = Instant.now();
        }
    }

}
