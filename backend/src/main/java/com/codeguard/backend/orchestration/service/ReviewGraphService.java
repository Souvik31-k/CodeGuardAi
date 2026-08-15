package com.codeguard.backend.orchestration.service;

import java.util.List;
import java.util.Map;

import org.bsc.langgraph4j.CompiledGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.codeguard.backend.model.ReviewRun;
import com.codeguard.backend.orchestration.model.AgentFinding;
import com.codeguard.backend.orchestration.model.ChangedFile;
import com.codeguard.backend.orchestration.model.FileClassification;
import com.codeguard.backend.orchestration.model.ReviewResult;
import com.codeguard.backend.orchestration.model.ReviewResult.ReviewStatus;
import com.codeguard.backend.orchestration.state.ReviewState;
import com.codeguard.backend.service.ReviewPersistenceService;

@Service
public class ReviewGraphService {

    private static final Logger log = LoggerFactory.getLogger(ReviewGraphService.class);

    private final CompiledGraph<ReviewState> compiledGraph;
    private final ReviewPersistenceService persistenceService;

    ReviewGraphService(CompiledGraph<ReviewState> compiledGraph, ReviewPersistenceService persistenceService) {
        this.compiledGraph = compiledGraph;
        this.persistenceService = persistenceService;
    }

    public ReviewResult startReview(ReviewRun reviewRun, List<ChangedFile> changedFiles) {

        /**
         * Creating an initial ReviewState
         * to pass the values from the ReviewRun to the ReviewState to invoke the graph
         */

        Map<String, Object> inti = Map.of(
                ReviewState.REVIEW_RUN_ID, reviewRun.getReviewRunId(),
                ReviewState.REPO_ID, reviewRun.getRepository().getRepoId(),
                ReviewState.PULL_REQUEST_NUMBER, reviewRun.getPullRequestNumber(),
                ReviewState.CHANGED_FILES, changedFiles);

        /**
         * Execute the complete review graph:
         *
         * Supervisor
         * ↓
         * Four parallel specialist agents
         * ↓
         * Aggregator
         *
         * The final state contains the aggregated ReviewResult.
         */

        ReviewState finalState = compiledGraph
                .invoke(inti)
                .orElseThrow(() -> new IllegalStateException("Graph execution returned final State"));

        for (FileClassification classification : finalState.classifications()) {

            log.info(" {} -> {}",
                    classification.getFilePath(),
                    classification.getCategory());
        }

        ReviewResult reviewResult = finalState.aggregatedResult();

        if (reviewResult == null) {

            throw new IllegalStateException("Review Graph completed without an aggregated result.");

        }

        String summary = reviewResult.getSummary();

        ReviewStatus status = reviewResult.getReviewStatus();

        List<AgentFinding> agentFindings = reviewResult.getFindings();

        log.info("Review Run {} completed with status {} and {} findings. Summary: {}",
                finalState.reviewRunId(),
                status,
                agentFindings.size(),
                summary);

        persistenceService.persist(reviewRun.getReviewRunId(), reviewResult);

        return reviewResult;

        // Graph execution and aggregated result persistence are complete.

    }
}
