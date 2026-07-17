package com.codeguard.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.codeguard.backend.model.AgentExecutionLog;

public interface AgentExecutionRepository extends JpaRepository<AgentExecutionLog, Long> {

}