package com.tchalanet.server.features.ops.draw;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Request body for generating draws for a date range (ops).
 *
 * <p>Tenant targeting is optional and resolved as: {@code tenantIds} if present, else the
 * single {@code tenantId} (back-compat), else <b>all active tenants</b> (mirrors the
 * scheduled {@code generateNext7Days} job). Generation is idempotent, so running across all
 * active tenants only creates the draws that are still missing.
 */
public record GenerateDrawsRequest(
    String tenantId,
    List<String> tenantIds,
    @NotNull LocalDate from,
    @NotNull LocalDate to,
    Boolean dryRun,
    Boolean force,
    String reason) {

  public GenerateDrawsRequest {
    Objects.requireNonNull(from, "from required");
    Objects.requireNonNull(to, "to required");
    if (from.isAfter(to)) throw new IllegalArgumentException("from must be <= to");
    if (dryRun == null) dryRun = Boolean.FALSE;
    if (force == null) force = Boolean.FALSE;
  }
}
