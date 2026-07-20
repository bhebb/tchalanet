package com.tchalanet.server.features.ops.draw;

import jakarta.validation.constraints.Min;
import java.util.List;

public record OpenTodayDrawsRequest(
    // Optional tenant targeting: these tenant codes if present, else ALL active tenants.
    List<String> tenantCodes,
    @Min(1) Integer limit,
    @Min(1) Integer lookaheadHours,
    @Min(0) Integer lagHours,
    Boolean dryRun) {
  public OpenTodayDrawsRequest {
    if (dryRun == null) dryRun = Boolean.FALSE;
  }
}
