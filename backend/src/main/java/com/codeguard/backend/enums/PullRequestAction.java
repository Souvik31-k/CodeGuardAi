package com.codeguard.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PullRequestAction {
    OPENED("opened"),
    SYNCHRONIZE("synchronize"),
    REOPENED("reopened"),
    CLOSED("closed");

    private final String value;

    PullRequestAction(String value) {
        this.value = value;
    }

    @JsonCreator
    public static PullRequestAction fromaction(String header) {
        for (PullRequestAction action : values()) {
            if (action.value.equalsIgnoreCase(header)) {
                return action;
            }
        }
        return null;
    }
}
