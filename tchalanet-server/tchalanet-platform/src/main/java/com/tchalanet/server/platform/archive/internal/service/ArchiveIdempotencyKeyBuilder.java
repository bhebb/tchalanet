package com.tchalanet.server.platform.archive.internal.service;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Builds stable idempotency keys for archive runs.
 *
 * <p>Key format: {@code {dataset}:{tenant_or_global}:{period_start}:{period_end}}
 * Per-dataset segment keys add {@code :{segment_no}}.
 */
public final class ArchiveIdempotencyKeyBuilder {

  private ArchiveIdempotencyKeyBuilder() {}

  /** Top-level run key covering all datasets for a period. */
  public static String forRun(LocalDate periodStart, LocalDate periodEnd) {
    return "run:global:%s:%s".formatted(periodStart, periodEnd);
  }

  /** Per-dataset key for one (dataset, tenant, period) combination. */
  public static String forDataset(String dataset, UUID tenantId, LocalDate periodStart, LocalDate periodEnd) {
    String tenantPart = tenantId != null ? tenantId.toString() : "global";
    return "%s:%s:%s:%s".formatted(dataset, tenantPart, periodStart, periodEnd);
  }

  /** Per-segment key for segmented exports of large datasets. */
  public static String forSegment(String dataset, UUID tenantId,
      LocalDate periodStart, LocalDate periodEnd, int segmentNo) {
    return "%s:%d".formatted(forDataset(dataset, tenantId, periodStart, periodEnd), segmentNo);
  }
}
