package com.codeguard.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.codeguard.backend.model.Findings;

public interface FindingsRepository extends JpaRepository<Findings, Long> {

}
