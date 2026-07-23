package com.codeguard.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.codeguard.backend.model.CodeRepository;
import com.codeguard.backend.model.ReviewRun;

public interface ReviewRunRepository extends JpaRepository<ReviewRun, Long> {
    Optional<ReviewRun> findByRepositoryAndCommitSha(CodeRepository repository, String commitSha);

}
