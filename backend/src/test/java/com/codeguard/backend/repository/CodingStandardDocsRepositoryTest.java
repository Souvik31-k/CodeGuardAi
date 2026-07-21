package com.codeguard.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import com.codeguard.backend.model.CodeRepository;
import com.codeguard.backend.model.CodingStandardDocs;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CodingStandardDocsRepositoryTest {
    private CodeRepositoryRepository codeRepository;
    private CodingStandardDocsRepository repository;

    @Autowired
    CodingStandardDocsRepositoryTest(CodingStandardDocsRepository repository, CodeRepositoryRepository codeRepository) {
        this.repository = repository;
        this.codeRepository = codeRepository;
    }

    @Test
    public void saveDocs() {
        CodeRepository repo = new CodeRepository();
        repo.setGithubRepoId(123456L);
        repo.setRepoName("CodeGuardAiPlatform");
        repo.setUserLogin("Souvik");
        repo.setActive(true);
        repo.setWebhookSecretEncrypted("encrypted-Secret");
        repo.setMinimumSeverity(CodeRepository.Severity.MEDIUM);

        CodeRepository obj = codeRepository.save(repo);
        CodingStandardDocs docs = new CodingStandardDocs();
        docs.setFileName("H1AutoCodeStandard.java");
        docs.setActive(true);
        docs.setRepository(obj);

        CodingStandardDocs saveDocs = repository.save(docs);

        Optional<CodingStandardDocs> res = repository.findById(saveDocs.getDocsId());

        assertThat(res).isPresent();
        assertThat(res.get().getFileName()).isEqualTo("H1AutoCodeStandard.java");
        assertThat(res.get().isActive()).isTrue();
        assertThat(res.get().getRepository().getRepoId()).isEqualTo(obj.getRepoId());
        assertThat(res.get().getRepository().getRepoName()).isEqualTo(obj.getRepoName());
    }
}
