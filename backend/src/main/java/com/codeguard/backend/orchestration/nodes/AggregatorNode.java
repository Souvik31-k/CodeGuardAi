package com.codeguard.backend.orchestration.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.bsc.langgraph4j.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.codeguard.backend.enums.AgentType;
import com.codeguard.backend.orchestration.model.AgentFinding;
import com.codeguard.backend.orchestration.model.ReviewResult;
import com.codeguard.backend.orchestration.model.SpecialistResult;
import com.codeguard.backend.orchestration.model.ReviewResult.ReviewStatus;
import com.codeguard.backend.orchestration.model.SpecialistResult.SpecialistStatus;
import com.codeguard.backend.orchestration.state.ReviewState;

@Component
public class AggregatorNode implements NodeAction<ReviewState> {

        private static final int EXPECTED_COUNT = 4;
        private static final Logger log = LoggerFactory.getLogger(AggregatorNode.class);
        private static final Set<AgentType> EXPECTED_SPECIALISTS = Set.of(
                        AgentType.SECURITY,
                        AgentType.QUALITY,
                        AgentType.TEST,
                        AgentType.DOCUMENTATION);

        @Override
        public Map<String, Object> apply(ReviewState state) throws Exception {

                List<SpecialistResult> results = new ArrayList<>();

                SpecialistResult documentationResult = state.documentationResult();
                if (documentationResult != null) {
                        results.add(documentationResult);
                }

                SpecialistResult securityResult = state.securityResult();
                if (securityResult != null) {
                        results.add(securityResult);
                }

                SpecialistResult qualityResult = state.qualityResult();
                if (qualityResult != null) {
                        results.add(qualityResult);
                }

                SpecialistResult testResult = state.testResult();
                if (testResult != null) {
                        results.add(testResult);
                }

                List<AgentFinding> findings = results.stream()
                                .filter((specialistResult) -> specialistResult.getFindings() != null)
                                .flatMap((specialistResult) -> specialistResult.getFindings().stream())
                                .toList();

                long completed = results.stream()
                                .filter((specialistResult) -> specialistResult
                                                .getStatus() == SpecialistStatus.COMPLETED)
                                .count();

                long failed = results.stream()
                                .filter((specialistResult) -> specialistResult.getStatus() == SpecialistStatus.FAILED
                                                || specialistResult.getStatus() == SpecialistStatus.TIMEOUT)
                                .count();

                ReviewStatus overallStatus;

                if (results.size() < EXPECTED_COUNT) {

                        overallStatus = ReviewStatus.PARTIALLY_COMPLETED;

                        logMissingAgents(state, results);

                }

                else if (failed == 0) {

                        overallStatus = ReviewStatus.COMPLETED;
                }

                else if (completed > 0 && failed > 0) {

                        overallStatus = ReviewStatus.PARTIALLY_COMPLETED;

                        logFailedAgents(state, results);

                }

                else {

                        overallStatus = ReviewStatus.FAILED;

                        logFailedAgents(state, results);
                }

                String summaryResponse = buildSummary(results, overallStatus);

                ReviewResult finalResult = new ReviewResult(findings, results, overallStatus, summaryResponse);

                return Map.of(
                                ReviewState.AGGREGATED_RESULT,
                                finalResult);
        }

        private String buildSummary(List<SpecialistResult> results,
                        ReviewStatus overallStatus) {

                StringBuilder summary = new StringBuilder();

                if (overallStatus == ReviewStatus.COMPLETED) {

                        summary.append("Review completed with all specialist agents completing successfully.");

                } else if (overallStatus == ReviewStatus.PARTIALLY_COMPLETED) {

                        summary.append("Review completed with partial specialist execution");

                } else {

                        summary.append("Review failed because all specialist agents failed or timed out.");

                }

                summary.append("\n");

                for (SpecialistResult result : results) {

                        int findingCount = result.getFindings() == null
                                        ? 0
                                        : result.getFindings().size();
                        summary.append(result.getAgentType())
                                        .append(": ")
                                        .append(result.getStatus());

                        if (result.getStatus() == SpecialistResult.SpecialistStatus.COMPLETED) {
                                summary.append(" (")
                                                .append(findingCount)
                                                .append(" findings)");
                        } else if (result.getFailureReason() != null) {
                                summary.append(" - ")
                                                .append(result.getFailureReason());
                        }

                        summary.append(". ");

                }

                long totalFinding = results.stream()
                                .map((result) -> result.getFindings())
                                .filter((list) -> list != null)
                                .mapToLong((list) -> list.size())
                                .sum();

                summary.append("\nTotal findings: ")
                                .append(totalFinding)
                                .append(".");

                return summary.toString();
        }

        private void logMissingAgents(
                        ReviewState state,
                        List<SpecialistResult> results) {

                Set<AgentType> present = results.stream()
                                .map((result) -> result.getAgentType())
                                .collect(Collectors.toSet());

                for (AgentType agent : EXPECTED_SPECIALISTS) {

                        if (!present.contains(agent)) {

                                log.warn(
                                                "Missing {} agent result for Review Run Id: {}."
                                                                + "Possible graph wiring or specialist execution issue.",
                                                agent,
                                                state.reviewRunId());
                        }
                }
        }

        private void logFailedAgents(
                        ReviewState state,
                        List<SpecialistResult> results) {

                results.stream()
                                .filter(result -> result.getStatus() == SpecialistStatus.FAILED
                                                || result.getStatus() == SpecialistStatus.TIMEOUT)
                                .forEach(result -> log.warn(
                                                "{} agent {} for ReviewRun {}. Reason: {}",
                                                result.getAgentType(),
                                                result.getStatus(),
                                                state.reviewRunId(),
                                                result.getFailureReason()));
        }

}
