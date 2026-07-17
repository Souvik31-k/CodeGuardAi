package com.codeguard.backend.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
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
import com.fasterxml.jackson.databind.JsonNode;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Findings {

    public enum AgentType {
        SECURITY,
        QUALITY,
        DOCUMENTATION,
        TESTING
    }

    public enum Severity {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long findingId;

    @Enumerated(EnumType.STRING)
    private AgentType agentType;

    @CreationTimestamp
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    private Severity severity;
    private String title;

    private String filePath;
    private Integer lineNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode details;

    @ManyToOne
    @JoinColumn(name = "review_run_id")
    private ReviewRun reviewRun;
}
