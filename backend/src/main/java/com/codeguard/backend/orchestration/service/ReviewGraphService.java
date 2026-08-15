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

@Service
public class ReviewGraphService {

    private static final Logger log = LoggerFactory.getLogger(ReviewGraphService.class);

    private final CompiledGraph<ReviewState> compiledGraph;

    ReviewGraphService(CompiledGraph<ReviewState> compiledGraph) {
        this.compiledGraph = compiledGraph;
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
         * Here the Supervisor Node is called to send the llm request.
         * Classification of the files
         */
        ReviewState finalState = compiledGraph
                .invoke(inti)
                .orElseThrow(() -> new IllegalStateException("Graph execution returned final State"));

        ReviewResult reviewResult = finalState.aggregatedResult();

        if (reviewResult == null) {

            throw new IllegalStateException("Review Graph completed without an aggregated result.");

        }

        String summary = reviewResult.getSummary();

        ReviewStatus status = reviewResult.getReviewStatus();

        List<AgentFinding> agentFindings = reviewResult.getFindings();

        for (FileClassification classification : finalState.classifications()) {

            log.info(" {} -> {}",
                    classification.getFilePath(),
                    classification.getCategory());
        }

        log.info("Review Run {} completed with status {} and {} findings. Summary{}",
                finalState.reviewRunId(),
                status,
                agentFindings.size(),
                summary);
        return reviewResult;
        // phase 4:
        //
        // persist classification
        // dispatch Speacialist agents
        // update ReviewRun Status

    }
}
