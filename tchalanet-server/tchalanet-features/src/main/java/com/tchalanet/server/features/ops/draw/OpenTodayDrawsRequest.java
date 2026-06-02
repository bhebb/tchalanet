package com.tchalanet.server.features.ops.draw;

import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record OpenTodayDrawsRequest(
    // Optional tenant targeting: these tenants if present, else ALL active tenants.
    List<String> tenantIds,
    Instant now,
    LocalDate drawDate,
    @Min(1) Integer limit,
    Boolean dryRun
) {
    public OpenTodayDrawsRequest {
        if (dryRun == null) dryRun = Boolean.FALSE;
    }
}
