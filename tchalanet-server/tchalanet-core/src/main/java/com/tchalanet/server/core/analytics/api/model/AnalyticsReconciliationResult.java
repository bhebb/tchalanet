package com.tchalanet.server.core.analytics.api.model;

import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Immutable outcome returned by a tenant-scoped analytics reconciliation run. */
public record AnalyticsReconciliationResult(
    UUID runId,
    TenantId tenantId,
    LocalDate from,
    LocalDate to,
    AnalyticsReconciliationMode mode,
    AnalyticsReconciliationStatus status,
    List<AnalyticsReconciliationMismatch> mismatches,
    Instant completedAt) {

  public AnalyticsReconciliationResult {
    mismatches = List.copyOf(mismatches);
  }
}
