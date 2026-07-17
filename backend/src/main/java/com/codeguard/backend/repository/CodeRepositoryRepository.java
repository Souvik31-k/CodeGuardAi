package com.codeguard.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.codeguard.backend.model.CodeRepository;

public interface CodeRepositoryRepository extends JpaRepository<CodeRepository, Long> {

}