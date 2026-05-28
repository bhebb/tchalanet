package com.tchalanet.server.core.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Sensitive JPA update conventions")
class SensitiveJpaUpdateConventionTest {

    private static final Path SOURCE_ROOT = Path.of("src/main/java");
    private static final Set<String> ALLOWED_REBUILD_SAVE_PATHS = Set.of(
        "com/tchalanet/server/core/draw/internal/infra/persistence/adapter/DrawLifecycleJpaAdapter.java",
        "com/tchalanet/server/core/ledger/internal/infra/persistence/JpaLedgerRepositoryAdapter.java",
        "com/tchalanet/server/core/limitpolicy/internal/infra/persistence/assignment/adapter/LimitAssignmentRepositoryAdapter.java",
        "com/tchalanet/server/core/outlet/internal/infra/persistence/adapter/OutletPersistenceAdapter.java"
    );
    private static final Set<String> GUARDED_SQL_WRITERS = Set.of(
        "com/tchalanet/server/core/drawresult/internal/infra/persistence/adapter/DrawResultWriterJdbcAdapter.java",
        "com/tchalanet/server/core/draw/internal/infra/persistence/adapter/DrawLifecycleJpaAdapter.java",
        "com/tchalanet/server/core/ledger/internal/infra/persistence/JpaLedgerRepositoryAdapter.java"
    );

    @Test
    @DisplayName("rebuild-and-save is allowlisted only for create-only or append-only paths")
    void rebuildAndSaveIsAllowlistedOnly() throws IOException {
        try (var files = Files.walk(SOURCE_ROOT)) {
            var offenders = files
                .filter(path -> path.toString().endsWith(".java"))
                .filter(SensitiveJpaUpdateConventionTest::containsRebuildSave)
                .map(SOURCE_ROOT::relativize)
                .map(Path::toString)
                .filter(path -> !ALLOWED_REBUILD_SAVE_PATHS.contains(path))
                .sorted()
                .collect(Collectors.toList());

            assertThat(offenders)
                .as("Sensitive updates must load managed entities; add an explicit allowlist entry only for create-only or append-only paths.")
                .isEmpty();
        }
    }

    @Test
    @DisplayName("guarded SQL allowlist documents status/version or append-only protection")
    void guardedSqlAllowlistIsDocumentedInCode() throws IOException {
        for (String writer : GUARDED_SQL_WRITERS) {
            String source = Files.readString(SOURCE_ROOT.resolve(writer));

            if (writer.contains("DrawResultWriterJdbcAdapter")) {
                assertThat(source).contains("version = draw_result.version + 1");
                assertThat(source).contains("CONFIRMED");
                assertThat(source).contains("OVERRIDDEN");
            } else if (writer.contains("DrawLifecycleJpaAdapter")) {
                assertThat(source).contains("bulkOpen");
                assertThat(source).contains("bulkClose");
            } else if (writer.contains("JpaLedgerRepositoryAdapter")) {
                assertThat(source).contains("existsById");
                assertThat(source).contains("Ledger entry id already exists");
            }
        }
    }

    private static boolean containsRebuildSave(Path path) {
        try {
            String source = Files.readString(path);
            return source.contains("save(mapper.toEntity(")
                || source.contains("save(mapper.toNewEntity(")
                || source.contains("repository.save(mapper.toEntity(")
                || source.contains("repo.save(mapper.toEntity(")
                || source.contains("jpaRepo.save(mapper.toEntity(");
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read " + path, ex);
        }
    }
}
