package com.tchalanet.server.features.ops.draw;

import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.time.LocalDate;

public record OpenTodayDrawsRequest(
    Instant now,
    LocalDate drawDate,
    @Min(1) Integer limit,
    Boolean dryRun
) {
    public OpenTodayDrawsRequest {
        if (dryRun == null) dryRun = Boolean.FALSE;
    }
}
