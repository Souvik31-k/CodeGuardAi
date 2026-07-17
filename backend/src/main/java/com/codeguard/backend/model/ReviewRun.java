package com.codeguard.backend.model;

import java.time.Instant;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRun {

    public enum Status {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        PARTIALLY_COMPLETED,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewRunId;
    private Long pullRequestNumber;

    @ManyToOne
    @JoinColumn(name = "repo_id")
    private CodeRepository repository;

    @ManyToOne
    @JoinColumn(name = "docs_id")
    private CodingStandardDocs codingStandard;

    @OneToMany(mappedBy = "reviewRun")
    private List<Findings> findings;

    @OneToMany(mappedBy = "reviewRun")
    private List<AgentExecutionLog> logs;

    @CreationTimestamp
    private Instant createdAt;

    private Instant finishedAt;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String commitSha;

}
