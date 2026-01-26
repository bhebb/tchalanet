package com.tchalanet.server.features.ops.infra.web.draws.model;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Objects;

/** Request body for generating draws for a date range (ops). */
public record GenerateDrawsRequest(
    @NotNull String tenantId,
    @NotNull LocalDate from,
    @NotNull LocalDate to,
    Boolean dryRun,
    Boolean force) {

  public GenerateDrawsRequest {
    Objects.requireNonNull(tenantId, "tenantId required");
    Objects.requireNonNull(from, "from required");
    Objects.requireNonNull(to, "to required");
    if (from.isAfter(to)) throw new IllegalArgumentException("from must be <= to");
    if (dryRun == null) dryRun = Boolean.FALSE;
    if (force == null) force = Boolean.FALSE;
  }
}
