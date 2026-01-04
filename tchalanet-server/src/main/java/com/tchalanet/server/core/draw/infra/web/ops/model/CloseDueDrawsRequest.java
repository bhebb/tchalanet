package com.tchalanet.server.core.draw.infra.web.ops.model;

import jakarta.validation.constraints.Min;
import java.time.Instant;

/** Request body for closing due draws (ops). */
public record CloseDueDrawsRequest(Instant now, @Min(1) int limit, Boolean dryRun) {
  public CloseDueDrawsRequest {
    if (dryRun == null) dryRun = Boolean.FALSE;
    if (now == null) now = Instant.now();
  }
}
