package com.tchalanet.server.platform.archive.api.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Archived ticket retrieved from object storage.
 *
 * <p>V1: lines and charges are empty — only header fields from {@code sales_ticket} are exported.
 * V2 will add line/charge assembly once separate table providers are implemented.
 */
public record ArchivedTicketView(
    boolean found,
    UUID ticketId,
    String publicCode,
    UUID tenantId,
    Instant soldAt,
    String saleStatus,
    String resultStatus,
    String settlementStatus,
    String currency,
    Map<String, Object> rawHeader,    // full header row for downstream processing
    List<Map<String, Object>> lines,  // empty in V1
    List<Map<String, Object>> charges, // empty in V1
    ArchiveObjectMeta archiveMeta
) {

  public record ArchiveObjectMeta(
      UUID objectId,
      java.time.LocalDate periodStart,
      java.time.LocalDate periodEnd,
      int schemaVersion
  ) {}

  public static ArchivedTicketView notFound(UUID ticketId) {
    return new ArchivedTicketView(
        false, ticketId, null, null, null, null, null, null, null,
        null, List.of(), List.of(), null);
  }
}
