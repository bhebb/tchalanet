package com.tchalanet.server.core.draw.infra.web.ops.model;

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
    if (limit <= 0) limit = 2000;
    if (openHorizonHours < 0) openHorizonHours = 0;
    if (openLagHours < 0) openLagHours = 0;
    if (dryRun == null) dryRun = Boolean.FALSE;
    if (now == null) now = Instant.now();
  }
}
