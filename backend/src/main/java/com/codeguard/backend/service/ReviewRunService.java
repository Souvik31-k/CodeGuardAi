package com.codeguard.backend.service;

import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codeguard.backend.model.CodeRepository;
import com.codeguard.backend.model.ReviewRun;
import com.codeguard.backend.repository.ReviewRunRepository;

@Service
public class ReviewRunService {

    private static final Logger log = LoggerFactory.getLogger(ReviewRunService.class);

    private final ReviewRunRepository reviewRunRepository;

    public ReviewRunService(ReviewRunRepository reviewRunRepository) {
        this.reviewRunRepository = reviewRunRepository;
    }

    public ReviewRun createReviewRun(Long pullRequestNumber, String headSha, CodeRepository repository) {
        // String baseSha = event.getPullRequest().getBase().getSha();

        Optional<ReviewRun> existing = reviewRunRepository.findByRepositoryAndCommitSha(repository, headSha);

        if (existing.isPresent()) {

            log.info("ReviewRun already exists for PR #'{}' and SHA '{}' ", pullRequestNumber, headSha);

            // returning the same ReviewRun if it already exists in the DB.

            return existing.get();
        }
        ReviewRun review = new ReviewRun();

        review.setPullRequestNumber(pullRequestNumber);

        review.setRepository(repository);

        review.setStatus(ReviewRun.Status.PENDING);

        review.setCommitSha(headSha);

        // Another request may insert the same ReviewRun after our existence check.
        // If that happens, the database UNIQUE constraint prevents
        // duplicates.(repo,commitSha)
        // Reload and return the existing ReviewRun..
        try {
            return reviewRunRepository.save(review);
        } catch (DataIntegrityViolationException e) {

            log.info(
                    "Concurrent ReviewRun creation detected for repository {} and commit {}. Returning existing ReviewRun.",
                    repository.getGithubRepoId(),
                    headSha);

            return reviewRunRepository
                    .findByRepositoryAndCommitSha(
                            repository,
                            headSha)
                    .orElseThrow(() -> new IllegalStateException(
                            "ReviewRun was created concurrently but could not be retrieved", e));
        }

    }

    @Transactional
    public ReviewRun markFailed(ReviewRun reviewRun) {

        reviewRun.setStatus(ReviewRun.Status.FAILED);
        reviewRun.setFinishedAt(Instant.now());

        return reviewRunRepository.save(reviewRun);
    }

}
