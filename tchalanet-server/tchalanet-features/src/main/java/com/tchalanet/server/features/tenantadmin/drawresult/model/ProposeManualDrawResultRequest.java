package com.tchalanet.server.features.tenantadmin.drawresult.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Schema(description = "Tenant admin proposal for a manual draw result when provider fetch is unavailable.")
public record ProposeManualDrawResultRequest(
    @NotNull
    @Schema(description = "Draw calendar date.", example = "2026-05-02")
    LocalDate drawDate,

    @NotBlank
    @Schema(description = "Result slot key.", example = "NY_MID")
    String slotKey,

    @Schema(description = "Optional notes.", example = "Provider site unavailable; entered from official bulletin.")
    String notes,

    @Schema(description = "Pick 3 result.", example = "1-2-3")
    String pick3,

    @Schema(description = "Pick 4 result.", example = "1-2-3-4")
    String pick4
) {
    @AssertTrue(message = "at least one of pick3 or pick4 is required")
    public boolean hasAnyPick() {
        return (pick3 != null && !pick3.isBlank()) || (pick4 != null && !pick4.isBlank());
    }
}
