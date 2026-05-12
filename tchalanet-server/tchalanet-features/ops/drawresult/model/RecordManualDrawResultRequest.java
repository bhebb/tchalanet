package com.tchalanet.server.features.ops.drawresult.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Schema(description = "Request to record a manual draw result when provider fetch is unavailable.")
public record RecordManualDrawResultRequest(
    @NotBlank
    @Schema(description = "Tenant UUID. Required for audited tenant-scoped manual entry.")
    String tenantId,

    @NotNull
    @Schema(description = "Draw calendar date.", example = "2026-05-02")
    LocalDate drawDate,

    @NotBlank
    @Schema(description = "Result slot key.", example = "NY_MID")
    String slotKey,

    @NotBlank
    @Schema(description = "Operator/user identifier who recorded the result.", example = "ops@tchalanet.com")
    String recordedBy,

    @Schema(description = "Optional notes.", example = "Provider site unavailable; entered from official bulletin.")
    String notes,

    @Schema(description = "Pick 3 result.", example = "1-2-3")
    String pick3,

    @Schema(description = "Pick 4 result.", example = "1-2-3-4")
    String pick4,

    @Schema(description = "Force manual record when allowed by command rules.", example = "false")
    boolean force,

    @Schema(description = "Required when force=true.", example = "Manual correction approved by platform ops")
    String reason
) {
    @AssertTrue(message = "at least one of pick3 or pick4 is required")
    public boolean hasAnyPick() {
        return (pick3 != null && !pick3.isBlank()) || (pick4 != null && !pick4.isBlank());
    }

    @AssertTrue(message = "reason is required when force is true")
    public boolean isReasonValidForForce() {
        return !force || (reason != null && !reason.isBlank());
    }
}
