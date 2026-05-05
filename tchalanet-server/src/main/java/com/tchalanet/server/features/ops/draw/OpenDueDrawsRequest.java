package com.tchalanet.server.features.ops.draw;

import jakarta.validation.constraints.Min;
import java.time.Instant;

/** Request body for opening due draws (ops). */
public record OpenDueDrawsRequest(
    Instant now,
    @Min(1) int limit,
    @Min(0) int openHorizonHours,
    @Min(0) int openLagHours,
    Boolean dryRun) {

  public OpenDueDrawsRequest {
    if (dryRun == null) dryRun = Boolean.FALSE;
    if (now == null) now = Instant.now();
  }
}
