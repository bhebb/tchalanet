package com.tchalanet.server.platform.archive.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.platform.archive.api.model.ArchiveObjectRowView;
import com.tchalanet.server.platform.archive.internal.config.ArchiveProperties;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveLookupIndexJdbcRepository;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveObjectJdbcRepository;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveRestoreAuditLogJdbcRepository;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveRestoreAuditLogJdbcRepository.RestoreAuditRow;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveRestoreRunJdbcRepository;
import com.tchalanet.server.platform.archive.internal.storage.ArchiveStoragePort;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

class ArchiveRestoreServiceTest {

  private static final UUID RUN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID OBJECT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID TENANT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID OTHER_TENANT_ID =
      UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID ENTITY_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
  private static final LocalDate FROM = LocalDate.of(2025, 1, 1);
  private static final LocalDate TO = LocalDate.of(2025, 2, 1);

  @Test
  void restoresOnlyRequestedTenantAndDateWindow() throws Exception {
    var harness = harness(10);
    when(harness.storage.exists("archive/audit.jsonl.gz")).thenReturn(true);
    when(harness.storage.openRead("archive/audit.jsonl.gz"))
        .thenReturn(
            new ByteArrayInputStream(
                archive(
                    row(TENANT_ID, "2025-01-15T12:00:00Z"),
                    row(OTHER_TENANT_ID, "2025-01-15T12:00:00Z"),
                    row(TENANT_ID, "2025-02-01T00:00:00Z"))));

    UUID restoreRunId =
        harness.service.restoreAuditLog(
            TENANT_ID, "TICKET", ENTITY_ID, FROM, TO, TENANT_ID, "investigate archive record");

    assertThat(restoreRunId).isEqualTo(RUN_ID);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<RestoreAuditRow>> rows = ArgumentCaptor.forClass(List.class);
    verify(harness.restoreAuditRepo).insertBatch(eq(RUN_ID), eq(OBJECT_ID), rows.capture());
    assertThat(rows.getValue()).hasSize(1);
    assertThat(rows.getValue().getFirst().tenantId()).isEqualTo(TENANT_ID);
    verify(harness.restoreRunRepo).incrementRowCount(RUN_ID, 1);
  }

  @Test
  void refusesRestoreThatExceedsConfiguredRowLimit() throws Exception {
    var harness = harness(1);
    when(harness.storage.exists("archive/audit.jsonl.gz")).thenReturn(true);
    when(harness.storage.openRead("archive/audit.jsonl.gz"))
        .thenReturn(
            new ByteArrayInputStream(
                archive(
                    row(TENANT_ID, "2025-01-15T12:00:00Z"),
                    row(TENANT_ID, "2025-01-16T12:00:00Z"))));

    assertThatThrownBy(
            () ->
                harness.service.restoreAuditLog(
                    TENANT_ID,
                    "TICKET",
                    ENTITY_ID,
                    FROM,
                    TO,
                    TENANT_ID,
                    "investigate archive record"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("max rows per run");
  }

  private static Harness harness(long maxRows) {
    var restoreRunRepo = mock(ArchiveRestoreRunJdbcRepository.class);
    var restoreAuditRepo = mock(ArchiveRestoreAuditLogJdbcRepository.class);
    var lookupRepo = mock(ArchiveLookupIndexJdbcRepository.class);
    var objectRepo = mock(ArchiveObjectJdbcRepository.class);
    var storage = mock(ArchiveStoragePort.class);
    when(restoreRunRepo.countActive()).thenReturn(0);
    when(restoreRunRepo.insert(any(), any(), any(), eq("[]"))).thenReturn(RUN_ID);
    when(lookupRepo.findByEntity("audit_log", "TICKET", ENTITY_ID))
        .thenReturn(List.of(Map.of("archive_object_id", OBJECT_ID)));
    when(objectRepo.findById(OBJECT_ID))
        .thenReturn(
            Optional.of(
                new ArchiveObjectRowView(
                    OBJECT_ID,
                    RUN_ID,
                    "audit_log",
                    null,
                    FROM,
                    TO,
                    null,
                    null,
                    0,
                    "archive/audit.jsonl.gz",
                    "jsonl",
                    "gzip",
                    2,
                    100,
                    "checksum",
                    1,
                    "VERIFIED",
                    Instant.parse("2025-02-01T00:00:00Z"))));
    var props =
        new ArchiveProperties(
            true,
            new ArchiveProperties.Storage(
                "local",
                "./archive-data",
                "tch-archive",
                "archive",
                1_000,
                null,
                "auto",
                null,
                null),
            new ArchiveProperties.Restore(java.time.Duration.ofDays(7), maxRows, 5),
            new ArchiveProperties.Cleanup(false, "DRY_RUN", 12, List.of("audit_log")));
    var service =
        new ArchiveRestoreService(
            props,
            restoreRunRepo,
            restoreAuditRepo,
            lookupRepo,
            objectRepo,
            storage,
            new JsonUtils(JsonMapper.builder().build()));
    return new Harness(service, restoreRunRepo, restoreAuditRepo, storage);
  }

  private static Map<String, Object> row(UUID tenantId, String occurredAt) {
    return Map.of(
        "id",
        UUID.randomUUID().toString(),
        "tenant_id",
        tenantId.toString(),
        "entity_type",
        "TICKET",
        "entity_id",
        ENTITY_ID.toString(),
        "occurred_at",
        occurredAt,
        "action",
        "UPDATE");
  }

  private static byte[] archive(Map<String, Object>... rows) throws IOException {
    var output = new ByteArrayOutputStream();
    try (var gzip = new GZIPOutputStream(output)) {
      var json = new JsonUtils(JsonMapper.builder().build());
      for (Map<String, Object> row : rows) {
        gzip.write(json.toJson(row).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        gzip.write('\n');
      }
    }
    return output.toByteArray();
  }

  private record Harness(
      ArchiveRestoreService service,
      ArchiveRestoreRunJdbcRepository restoreRunRepo,
      ArchiveRestoreAuditLogJdbcRepository restoreAuditRepo,
      ArchiveStoragePort storage) {}
}
