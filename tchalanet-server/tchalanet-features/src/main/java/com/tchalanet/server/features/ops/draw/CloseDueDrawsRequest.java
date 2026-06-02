package com.tchalanet.server.features.ops.draw;

import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.List;

/** Request body for closing due draws (ops). */
public record CloseDueDrawsRequest(
    // Optional tenant targeting: these tenant codes if present, else ALL active tenants.
    List<String> tenantCodes,
    Instant now,
    @Min(1) int limit,
    Boolean dryRun) {
  public CloseDueDrawsRequest {
    if (dryRun == null) dryRun = Boolean.FALSE;
  }
}
