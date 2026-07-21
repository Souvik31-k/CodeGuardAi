package com.codeguard.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum GithubEvent {
    PING("ping"),
    PULL_REQUEST("pull_request"),
    PUSH("push");

    private final String value;

    GithubEvent(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @JsonCreator
    public static GithubEvent fromheader(String header) {
        for (GithubEvent event : values()) {
            if (event.value.equalsIgnoreCase(header)) {
                return event;
            }
        }
        return null;
    }
}
