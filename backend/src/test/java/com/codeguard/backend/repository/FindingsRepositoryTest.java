package com.codeguard.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import com.codeguard.backend.enums.AgentType;
import com.codeguard.backend.enums.Severity;
import com.codeguard.backend.model.CodeRepository;
import com.codeguard.backend.model.Findings;
import com.codeguard.backend.model.ReviewRun;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class FindingsRepositoryTest {
    private CodeRepositoryRepository codeRepository;
    private ReviewRunRepository reviewRunRepository;
    private FindingsRepository findingsRepository;

    @Autowired
    FindingsRepositoryTest(CodeRepositoryRepository codeRepository, ReviewRunRepository reviewRunRepository,
            FindingsRepository findingsRepository) {
        this.codeRepository = codeRepository;
        this.reviewRunRepository = reviewRunRepository;
        this.findingsRepository = findingsRepository;
    }

    @Test
    public void saveFindings() {
        CodeRepository codeRepo = new CodeRepository();
        codeRepo.setGithubRepoId(123456L);
        codeRepo.setRepoName("CodeGuardAiPlatform");
        codeRepo.setUserLogin("Souvik");
        codeRepo.setActive(true);
        codeRepo.setWebhookSecretEncrypted("encrypted-Secret");
        codeRepo.setMinimumSeverity(CodeRepository.Severity.MEDIUM);

        CodeRepository saveCodeRepo = codeRepository.save(codeRepo);

        ReviewRun review = new ReviewRun();
        review.setPullRequestNumber(1234567L);
        review.setStatus(ReviewRun.Status.PARTIALLY_COMPLETED);
        review.setRepository(saveCodeRepo);
        review.setCommitSha("#HL45bgh");
        review.setCodingStandard(null);

        ReviewRun saveReview = reviewRunRepository.save(review);

        // Creating the JsonB object
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode details = mapper.createObjectNode();
        details.put("rule", "NullPointerChecker");
        details.put("solution", "Handle null values for the variables");
        details.put("confidence", "HIGH");

        Findings find = new Findings();
        find.setAgentType(AgentType.SECURITY);
        find.setSeverity(Severity.HIGH);
        find.setTitle("Null pointer Possiblity");
        find.setFilePath("src/main/java/Test.java");
        find.setLineNumber(22);
        find.setDetails(details);
        find.setReviewRun(saveReview);

        // Saving Findings object using JpaRepo
        Findings saveFindings = findingsRepository.save(find);

        Optional<Findings> result = findingsRepository.findById(saveFindings.getFindingId());

        // Assert

        assertThat(result).isPresent();

        assertThat(result.get().getTitle()).isEqualTo("Null pointer Possiblity");

        assertThat(result.get().getAgentType()).isEqualTo(AgentType.SECURITY);

        assertThat(result.get().getSeverity()).isEqualTo(Severity.HIGH);

        assertThat(result.get().getFilePath()).isEqualTo("src/main/java/Test.java");

        assertThat(result.get().getLineNumber()).isEqualTo(22);

        assertThat(result.get().getCreatedAt()).isNotNull();

        assertThat(result.get().getReviewRun().getReviewRunId()).isEqualTo(saveReview.getReviewRunId());

        assertThat(result.get().getDetails().get("rule").asText()).isEqualTo("NullPointerChecker");

        assertThat(result.get().getDetails().get("solution").asText())
                .isEqualTo("Handle null values for the variables");

        assertThat(result.get().getDetails().get("confidence").asText()).isEqualTo("HIGH");
    }
}
