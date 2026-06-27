package com.tchalanet.server.platform.archive.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.platform.archive.api.ArchiveDatasetProvider;
import com.tchalanet.server.platform.archive.api.model.ArchiveDatasetKey;
import com.tchalanet.server.platform.archive.api.model.ArchiveDatasetPlan;
import com.tchalanet.server.platform.archive.api.model.ArchiveExportRequest;
import com.tchalanet.server.platform.archive.api.model.ArchiveExportResult;
import com.tchalanet.server.platform.archive.api.model.ArchiveLookupEntry;
import com.tchalanet.server.platform.archive.api.model.ArchiveLookupRequest;
import com.tchalanet.server.platform.archive.api.model.ArchiveLookupResult;
import com.tchalanet.server.platform.archive.api.model.ArchivePeriod;
import com.tchalanet.server.platform.archive.api.model.TriggerArchiveRunRequest;
import com.tchalanet.server.platform.archive.internal.config.ArchiveProperties;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveLookupIndexJdbcRepository;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveObjectJdbcRepository;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveRunJdbcRepository;
import com.tchalanet.server.platform.archive.internal.storage.LocalFileArchiveStorageAdapter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("ArchiveRunExecutor")
class ArchiveRunExecutorTest {

  private static final UUID RUN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID TENANT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID ENTITY_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

  @TempDir
  Path archiveRoot;

  @Test
  @DisplayName("verifies compressed archive object before marking it VERIFIED")
  void verifiesCompressedArchiveObjectBeforeMarkingItVerified() throws Exception {
    var provider = new FakeProvider(2, 2);
    var harness = new Harness(archiveRoot, provider);

    harness.executor.execute(request(), USER_ID);

    var objectId = harness.insertedObjectId.get();
    assertThat(objectId).isNotNull();
    assertThat(harness.insertedObjectUri.get()).isNotBlank();
    assertThat(readArchiveLines(archiveRoot, harness.insertedObjectUri.get()))
        .hasSize(2)
        .allMatch(line -> line.contains("\"table\":\"audit_log\""));

    verify(harness.objectRepo).markVerified(objectId);
    verify(harness.guard).complete(RUN_ID);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<ArchiveLookupEntry>> lookupCaptor = ArgumentCaptor.forClass(List.class);
    verify(harness.lookupRepo).insertBatch(lookupCaptor.capture());
    assertThat(lookupCaptor.getValue())
        .singleElement()
        .satisfies(entry -> {
          assertThat(entry.tableName()).isEqualTo("audit_log");
          assertThat(entry.tenantId()).isEqualTo(TENANT_ID);
          assertThat(entry.entityType()).isEqualTo("TICKET");
          assertThat(entry.entityId()).isEqualTo(ENTITY_ID);
          assertThat(entry.archiveObjectId()).isEqualTo(objectId);
        });
  }

  @Test
  @DisplayName("marks object INVALID and fails run when provider count differs from writer count")
  void marksObjectInvalidAndFailsRunWhenProviderCountDiffersFromWriterCount() {
    var provider = new FakeProvider(2, 1);
    var harness = new Harness(archiveRoot, provider);

    assertThatThrownBy(() -> harness.executor.execute(request(), USER_ID))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("archive row-count mismatch");

    verify(harness.objectRepo).markInvalid(harness.insertedObjectId.get());
    verify(harness.guard).fail(eq(RUN_ID), any());
  }

  private TriggerArchiveRunRequest request() {
    return new TriggerArchiveRunRequest(
        "MONTHLY",
        LocalDate.of(2025, 1, 1),
        LocalDate.of(2025, 2, 1),
        "Monthly archive test");
  }

  private static List<String> readArchiveLines(Path root, String uri) throws Exception {
    try (var in = new GZIPInputStream(java.nio.file.Files.newInputStream(root.resolve(uri)));
        var reader = new java.io.BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      return reader.lines().toList();
    }
  }

  private static final class Harness {
    private final ArchiveRunGuard guard = mock(ArchiveRunGuard.class);
    private final ArchiveRunJdbcRepository runRepo = mock(ArchiveRunJdbcRepository.class);
    private final ArchiveObjectJdbcRepository objectRepo = mock(ArchiveObjectJdbcRepository.class);
    private final ArchiveLookupIndexJdbcRepository lookupRepo = mock(ArchiveLookupIndexJdbcRepository.class);
    private final AtomicReference<UUID> insertedObjectId = new AtomicReference<>();
    private final AtomicReference<String> insertedObjectUri = new AtomicReference<>();
    private final ArchiveRunExecutor executor;

    private Harness(Path archiveRoot, ArchiveDatasetProvider provider) {
      var props = new ArchiveProperties(
          true,
          new ArchiveProperties.Storage(
              "local", archiveRoot.toString(), "tchalanet-archive", "archive", 536870912L),
          new ArchiveProperties.Restore(java.time.Duration.ofDays(7), 1_000_000L, 5),
          new ArchiveProperties.Cleanup(false, "DRY_RUN", 12, List.of("audit_log")));
      var storage = new LocalFileArchiveStorageAdapter(props);
      var metrics = new ArchiveMetrics(new SimpleMeterRegistry());

      when(guard.beginOrResume(any(), any(), any(), any(), any()))
          .thenReturn(new ArchiveRunGuard.GuardResult(RUN_ID, ArchiveRunGuard.Decision.CREATED));
      when(runRepo.listRecent(anyInt())).thenReturn(List.of(Map.of(
          "id", RUN_ID,
          "status", "COMPLETED",
          "strategy", "MONTHLY",
          "trigger_type", "MANUAL",
          "idempotency_key", "monthly:2025-01-01:2025-02-01",
          "started_at", java.sql.Timestamp.from(Instant.parse("2025-01-01T00:00:00Z")))));
      when(objectRepo.insert(any(), any(), any(), any(), any(), any(), anyInt(), any(), any(Long.class),
          any(Long.class), any(), anyInt()))
          .thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            insertedObjectId.set(id);
            insertedObjectUri.set(invocation.getArgument(7));
            return id;
          });

      executor = new ArchiveRunExecutor(
          List.of(provider),
          guard,
          runRepo,
          objectRepo,
          lookupRepo,
          storage,
          JsonMapper.builder().build(),
          metrics);
    }
  }

  private static final class FakeProvider implements ArchiveDatasetProvider {
    private static final ArchiveDatasetKey KEY = ArchiveDatasetKey.of("audit_log", "Audit Log");

    private final long rowsToWrite;
    private final long rowsToReport;

    private FakeProvider(long rowsToWrite, long rowsToReport) {
      this.rowsToWrite = rowsToWrite;
      this.rowsToReport = rowsToReport;
    }

    @Override
    public ArchiveDatasetKey key() {
      return KEY;
    }

    @Override
    public ArchiveDatasetPlan plan(ArchivePeriod period, UUID tenantId) {
      return new ArchiveDatasetPlan(KEY, period, tenantId, rowsToWrite, true);
    }

    @Override
    public ArchiveExportResult export(ArchiveExportRequest request) {
      for (int i = 0; i < rowsToWrite; i++) {
        request.rowSink().accept(Map.of(
            "id", UUID.randomUUID().toString(),
            "table", "audit_log",
            "tenant_id", TENANT_ID.toString(),
            "entity_type", "TICKET",
            "entity_id", ENTITY_ID.toString(),
            "occurred_at", "2025-01-15T12:00:00Z"));
      }
      return new ArchiveExportResult(rowsToReport, 1);
    }

    @Override
    public ArchiveLookupResult lookup(ArchiveLookupRequest request) {
      return ArchiveLookupResult.notFound();
    }

    @Override
    public List<ArchiveLookupEntry> generateLookupRows(
        ArchivePeriod period, UUID tenantId, UUID archiveObjectId) {
      return List.of(new ArchiveLookupEntry(
          "audit_log",
          TENANT_ID,
          "TICKET",
          ENTITY_ID,
          null,
          null,
          Instant.parse("2025-01-15T12:00:00Z"),
          archiveObjectId,
          null,
          null));
    }
  }
}
