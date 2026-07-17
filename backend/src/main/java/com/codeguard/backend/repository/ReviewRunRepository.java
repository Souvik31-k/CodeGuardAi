package com.codeguard.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.codeguard.backend.model.ReviewRun;

public interface ReviewRunRepository extends JpaRepository<ReviewRun, Long> {

}
