package com.tchalanet.server.platform.archive.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveLookupIndexJdbcRepository;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveObjectJdbcRepository;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveRunJdbcRepository;
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
import tools.jackson.databind.json.JsonMapper;

class ArchiveServiceTest {

  private static final UUID RUN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID OBJECT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID TENANT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID OTHER_TENANT_ID =
      UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID TICKET_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

  @Test
  void returnsTypedTicketByIdWithArchiveMetadata() throws Exception {
    var harness = harness();
    when(harness.lookupRepo.findByEntity("sales_ticket", "TICKET", TICKET_ID))
        .thenReturn(List.of(Map.of("archive_object_id", OBJECT_ID)));
    when(harness.storage.exists("archive/ticket.jsonl.gz")).thenReturn(true);
    when(harness.storage.openRead("archive/ticket.jsonl.gz"))
        .thenReturn(new ByteArrayInputStream(archive(ticketRow(TENANT_ID))));

    var result = harness.service.findArchivedTicket(TENANT_ID, TICKET_ID);

    assertThat(result.found()).isTrue();
    assertThat(result.ticketId()).isEqualTo(TICKET_ID);
    assertThat(result.publicCode()).isEqualTo("PUB-555");
    assertThat(result.tenantId()).isEqualTo(TENANT_ID);
    assertThat(result.soldAt()).isEqualTo(Instant.parse("2025-01-15T12:00:00Z"));
    assertThat(result.saleStatus()).isEqualTo("APPROVED");
    assertThat(result.archiveMeta().objectId()).isEqualTo(OBJECT_ID);
    assertThat(result.archiveMeta().periodStart()).isEqualTo(LocalDate.of(2025, 1, 1));
    assertThat(result.lines()).isEmpty();
    assertThat(result.charges()).isEmpty();
  }

  @Test
  void doesNotReturnTicketFromAnotherTenantWhenLookingUpByPublicCode() throws Exception {
    var harness = harness();
    when(harness.lookupRepo.findByPublicCode("sales_ticket", "PUB-555"))
        .thenReturn(List.of(Map.of("archive_object_id", OBJECT_ID)));
    when(harness.storage.exists("archive/ticket.jsonl.gz")).thenReturn(true);
    when(harness.storage.openRead("archive/ticket.jsonl.gz"))
        .thenReturn(new ByteArrayInputStream(archive(ticketRow(OTHER_TENANT_ID))));

    var result = harness.service.findArchivedTicketByPublicCode(TENANT_ID, "PUB-555");

    assertThat(result.found()).isFalse();
    assertThat(result.ticketId()).isNull();
  }

  @Test
  void ignoresUnverifiedLookupObjects() {
    var harness = harness();
    when(harness.lookupRepo.findByEntity("sales_ticket", "TICKET", TICKET_ID))
        .thenReturn(List.of(Map.of("archive_object_id", OBJECT_ID)));
    when(harness.objectRepo.findById(OBJECT_ID))
        .thenReturn(Optional.of(object("INVALID", "sales_ticket")));

    var result = harness.service.findArchivedTicket(TENANT_ID, TICKET_ID);

    assertThat(result.found()).isFalse();
  }

  private static Harness harness() {
    var lookupRepo = mock(ArchiveLookupIndexJdbcRepository.class);
    var objectRepo = mock(ArchiveObjectJdbcRepository.class);
    var storage = mock(ArchiveStoragePort.class);
    when(objectRepo.findById(OBJECT_ID))
        .thenReturn(Optional.of(object("VERIFIED", "sales_ticket")));
    var service =
        new ArchiveService(
            mock(ArchiveRunExecutor.class),
            mock(ArchiveRunJdbcRepository.class),
            lookupRepo,
            objectRepo,
            storage,
            new JsonUtils(JsonMapper.builder().build()),
            mock(ArchiveMetrics.class));
    return new Harness(service, lookupRepo, objectRepo, storage);
  }

  private static com.tchalanet.server.platform.archive.api.model.ArchiveObjectRowView object(
      String status, String tableName) {
    return new com.tchalanet.server.platform.archive.api.model.ArchiveObjectRowView(
        OBJECT_ID,
        RUN_ID,
        tableName,
        TENANT_ID,
        LocalDate.of(2025, 1, 1),
        LocalDate.of(2025, 2, 1),
        null,
        null,
        0,
        "archive/ticket.jsonl.gz",
        "jsonl",
        "gzip",
        1,
        100,
        "checksum",
        1,
        status,
        Instant.parse("2025-02-01T00:00:00Z"));
  }

  private static Map<String, Object> ticketRow(UUID tenantId) {
    return Map.of(
        "id", TICKET_ID.toString(),
        "tenant_id", tenantId.toString(),
        "public_code", "PUB-555",
        "sold_at", "2025-01-15T12:00:00Z",
        "sale_status", "APPROVED",
        "result_status", "WON",
        "settlement_status", "PAID",
        "currency", "HTG");
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
      ArchiveService service,
      ArchiveLookupIndexJdbcRepository lookupRepo,
      ArchiveObjectJdbcRepository objectRepo,
      ArchiveStoragePort storage) {}
}
