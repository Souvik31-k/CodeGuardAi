package com.codeguard.backend.repository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import com.codeguard.backend.model.CodeRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CodeRepositoryRepositoryTest {
    private CodeRepositoryRepository repository;

    @Autowired
    CodeRepositoryRepositoryTest(CodeRepositoryRepository repository) {
        this.repository = repository;
    }

    @Test
    public void saveRepository() {
        CodeRepository repo = new CodeRepository();
        repo.setGithubRepoId(123456L);
        repo.setRepoName("CodeGuardAiPlatform");
        repo.setUserLogin("Souvik");
        repo.setActive(true);
        repo.setWebhookSecretEncrypted("encrypted-Secret");
        repo.setMinimumSeverity(CodeRepository.Severity.MEDIUM);

        CodeRepository obj = repository.save(repo);

        Optional<CodeRepository> res = repository.findById(obj.getRepoId());

        assertThat(res).isPresent();

        assertThat(res.get().getRepoId()).isNotNull();

        assertThat(res.get().getRepoName()).isEqualTo("CodeGuardAiPlatform");

        assertThat(res.get().getGithubRepoId()).isEqualTo(123456L);

        assertThat(res.get().getMinimumSeverity()).isEqualTo(CodeRepository.Severity.MEDIUM);

        assertThat(res.get().isActive()).isTrue();

        assertThat(res.get().getCreatedAt()).isNotNull();
    }
}
