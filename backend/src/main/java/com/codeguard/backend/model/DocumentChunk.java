package com.codeguard.backend.model;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chunkId;
    private Integer chunkIndex;
    private String chunkText;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(columnDefinition = "vector(512)")
    private float[] embeddings;

    @ManyToOne
    @JoinColumn(name = "docs_id")
    private CodingStandardDocs docs;

    @ManyToOne
    private CodeRepository repository;
}
