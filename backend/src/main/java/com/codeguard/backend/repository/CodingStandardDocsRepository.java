package com.codeguard.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.codeguard.backend.model.CodingStandardDocs;

public interface CodingStandardDocsRepository extends JpaRepository<CodingStandardDocs, Long> {

}
