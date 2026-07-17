package com.codeguard.backend.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgentExecutionLog {

    public enum AgentType {
        SECURITY,
        QUALITY,
        DOCUMENTATION,
        TESTING
    }

    public enum Status {
        RUNNING,
        SUCCESS,
        FAILED,
        TIMEOUT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long executionId;

    @Enumerated(EnumType.STRING)
    private AgentType agentType;

    @CreationTimestamp
    private Instant startedAt;

    private String errMessage;

    @Enumerated(EnumType.STRING)
    private Status status;
    private Instant finishedAt;

    @ManyToOne
    @JoinColumn(name = "review_run_id")
    private ReviewRun reviewRun;

}
