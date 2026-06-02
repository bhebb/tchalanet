package com.tchalanet.server.features.ops.draw;

import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record OpenTodayDrawsRequest(
    // Optional tenant targeting: these tenant codes if present, else ALL active tenants.
    List<String> tenantCodes,
    Instant now,
    LocalDate drawDate,
    @Min(1) Integer limit,
    Boolean dryRun
) {
    public OpenTodayDrawsRequest {
        if (dryRun == null) dryRun = Boolean.FALSE;
    }
}
