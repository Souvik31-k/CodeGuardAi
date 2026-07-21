package com.codeguard.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import com.codeguard.backend.model.CodeRepository;
import com.codeguard.backend.model.ReviewRun;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class ReviewRunRepositoryTest {
    private ReviewRunRepository repository;
    private CodeRepositoryRepository codeRepository;

    @Autowired
    ReviewRunRepositoryTest(CodeRepositoryRepository codeRepository, ReviewRunRepository repository) {
        this.repository = repository;
        this.codeRepository = codeRepository;
    }

    @Test
    public void save() {
        CodeRepository codeRepo = new CodeRepository();
        codeRepo.setGithubRepoId(123456L);
        codeRepo.setRepoName("CodeGuardAiPlatform");
        codeRepo.setUserLogin("Souvik");
        codeRepo.setActive(true);
        codeRepo.setWebhookSecretEncrypted("encrypted-Secret");
        codeRepo.setMinimumSeverity(CodeRepository.Severity.MEDIUM);

        CodeRepository codeRepositorySave = codeRepository.save(codeRepo);

        ReviewRun review = new ReviewRun();
        review.setPullRequestNumber(1234567L);
        review.setStatus(ReviewRun.Status.PARTIALLY_COMPLETED);
        review.setRepository(codeRepositorySave);
        review.setCommitSha("#HL45bgh");
        review.setCodingStandard(null); // can be nullable

        ReviewRun saveReview = repository.save(review);

        Optional<ReviewRun> result = repository.findById(saveReview.getReviewRunId());

        assertThat(result).isPresent();
        assertThat(result.get().getPullRequestNumber()).isEqualTo(1234567L);
        assertThat(result.get().getStatus()).isEqualTo(ReviewRun.Status.PARTIALLY_COMPLETED);
        assertThat(result.get().getCommitSha()).isEqualTo("#HL45bgh");
        assertThat(result.get().getCreatedAt()).isNotNull();
        assertThat(result.get().getRepository().getRepoId()).isEqualTo(codeRepositorySave.getRepoId());
        assertThat(result.get().getRepository().getRepoName()).isEqualTo("CodeGuardAiPlatform");
        assertThat(result.get().getCodingStandard()).isNull();

    }
}
