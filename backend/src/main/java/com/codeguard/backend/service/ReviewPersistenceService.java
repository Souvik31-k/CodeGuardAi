package com.codeguard.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codeguard.backend.model.Findings;
import com.codeguard.backend.model.ReviewRun;
import com.codeguard.backend.orchestration.model.AgentFinding;
import com.codeguard.backend.orchestration.model.ReviewResult;
import com.codeguard.backend.repository.FindingsRepository;
import com.codeguard.backend.repository.ReviewRunRepository;

@Service
public class ReviewPersistenceService {

    private final ReviewRunRepository reviewRunRepository;
    private final FindingsRepository findingsRepository;

    public ReviewPersistenceService(ReviewRunRepository reviewRunRepository, FindingsRepository findingsRepository) {

        this.reviewRunRepository = reviewRunRepository;
        this.findingsRepository = findingsRepository;

    }

    @Transactional
    public void persist(
            Long reviewRunId,
            ReviewResult reviewResult) {

        ReviewRun reviewRun = reviewRunRepository.findById(reviewRunId)
                .orElseThrow(() -> new IllegalStateException(
                        "Reviewn Run not found: " + reviewRunId));

        reviewRun.setStatus(mapStatus(reviewResult.getReviewStatus()));
        reviewRunRepository.save(reviewRun);

        // Converting the AgentFinding Type into Finding
        List<Findings> findings = reviewResult.getFindings()
                .stream()
                .map((agentFinding) -> mapFinding(agentFinding, reviewRun))
                .toList();

        findingsRepository.saveAll(findings);
    }

    private Findings mapFinding(
            AgentFinding agentFinding,
            ReviewRun reviewRun) {
        Findings findings = new Findings();

        findings.setAgentType(agentFinding.getAgentType());
        findings.setSeverity(agentFinding.getSeverity());
        findings.setTitle(agentFinding.getTitle());
        findings.setFilePath(agentFinding.getFilePath());
        findings.setLineNumber(agentFinding.getLineNumber());
        findings.setDetails(agentFinding.getDetails());
        findings.setReviewRun(reviewRun);

        return findings;
    }

    private ReviewRun.Status mapStatus(
            ReviewResult.ReviewStatus status) {
        return switch (status) {

            case COMPLETED ->
                ReviewRun.Status.COMPLETED;

            case PARTIALLY_COMPLETED ->
                ReviewRun.Status.PARTIALLY_COMPLETED;

            case FAILED ->
                ReviewRun.Status.FAILED;
        };
    }
}
