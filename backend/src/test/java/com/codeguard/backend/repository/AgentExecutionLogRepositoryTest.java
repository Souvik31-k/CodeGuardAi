package com.codeguard.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import com.codeguard.backend.model.AgentExecutionLog;
import com.codeguard.backend.model.CodeRepository;
import com.codeguard.backend.model.ReviewRun;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class AgentExecutionLogRepositoryTest {
    private CodeRepositoryRepository codeRepository;
    private AgentExecutionRepository repository;
    private ReviewRunRepository reviewRunRepository;

    @Autowired
    AgentExecutionLogRepositoryTest(CodeRepositoryRepository codeRepository, AgentExecutionRepository repository,
            ReviewRunRepository reviewRunRepository) {
        this.codeRepository = codeRepository;
        this.repository = repository;
        this.reviewRunRepository = reviewRunRepository;
    }

    @Test
    public void saveAgentExecutionLog() {
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
        review.setCodingStandard(null);

        ReviewRun saveReview = reviewRunRepository.save(review);

        AgentExecutionLog log = new AgentExecutionLog();
        log.setAgentType(AgentExecutionLog.AgentType.SECURITY);
        log.setErrMessage(null);
        log.setStatus(AgentExecutionLog.Status.RUNNING);
        log.setReviewRun(saveReview);

        AgentExecutionLog savedLog = repository.save(log);

        Optional<AgentExecutionLog> result = repository.findById(savedLog.getExecutionId());

        assertThat(result).isPresent();
        assertThat(result.get().getAgentType()).isEqualTo(AgentExecutionLog.AgentType.SECURITY);
        assertThat(result.get().getErrMessage()).isNull();
        assertThat(result.get().getStatus()).isEqualTo(AgentExecutionLog.Status.RUNNING);
        assertThat(result.get().getReviewRun().getReviewRunId()).isEqualTo(saveReview.getReviewRunId());
        assertThat(result.get().getReviewRun().getCommitSha()).isEqualTo("#HL45bgh");
        assertThat(result.get().getErrMessage()).isNull();

    }
}
