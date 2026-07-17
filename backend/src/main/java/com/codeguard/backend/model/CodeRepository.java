package com.codeguard.backend.model;

import java.time.Instant;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
public class CodeRepository {

    public enum Severity {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long repoId;
    private Long githubRepoId;
    private String repoName;
    private String userLogin;
    private boolean isActive;
    private String webhookSecretEncrypted;
    @Enumerated(EnumType.STRING)
    private Severity minimumSeverity;

    @CreationTimestamp
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;

    @OneToMany(mappedBy = "repository")
    private List<CodingStandardDocs> documents;

    @OneToMany(mappedBy = "repository")
    private List<ReviewRun> reviewRuns;
}
