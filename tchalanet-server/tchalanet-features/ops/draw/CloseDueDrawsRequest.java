package com.tchalanet.server.features.ops.draw;

import jakarta.validation.constraints.Min;
import java.time.Instant;

/** Request body for closing due draws (ops). */
public record CloseDueDrawsRequest(
    Instant now,
    @Min(1) int limit,
    Boolean dryRun) {
  public CloseDueDrawsRequest {
    if (dryRun == null) dryRun = Boolean.FALSE;
  }
}
