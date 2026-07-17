package com.codeguard.backend.model;

import java.time.Instant;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Entity;
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
public class CodingStandardDocs {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long docsId;
    private String fileName;
    @CreationTimestamp
    private Instant uploadedAt;
    private boolean isActive;

    @OneToMany(mappedBy = "codingStandard")
    private List<ReviewRun> reviewRuns;

    @ManyToOne
    @JoinColumn(name = "repo_id")
    private CodeRepository repository;
}
