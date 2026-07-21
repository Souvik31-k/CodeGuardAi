package com.codeguard.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.codeguard.backend.model.CodeRepository;

public interface CodeRepositoryRepository extends JpaRepository<CodeRepository, Long> {

    boolean existsByGithubRepoId(Long githubRepoId);

    Optional<CodeRepository> findByGithubRepoId(Long githubRepoId);

}