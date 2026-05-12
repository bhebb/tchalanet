package com.tchalanet.server.features.ops.drawresult.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Schema(description = "Request to override an existing draw result for a slot/date.")
public record OverrideDrawResultRequest(
    @NotBlank
    @Schema(description = "Tenant UUID. Required for audited tenant-scoped override.")
    String tenantId,

    @NotBlank
    @Schema(description = "Result slot key.", example = "NY_MID")
    String slotKey,

    @NotNull
    @Schema(description = "Draw calendar date.", example = "2026-05-02")
    LocalDate drawDate,

    @Schema(description = "Pick 3 result.", example = "1-2-3")
    String pick3,

    @Schema(description = "Pick 4 result.", example = "1-2-3-4")
    String pick4,

    @NotBlank
    @Schema(description = "Operational reason for override.", example = "Official provider correction")
    String reason,

    @Schema(description = "Whether override may replace protected result states.", example = "true")
    boolean force
) {
    @AssertTrue(message = "at least one of pick3 or pick4 is required")
    public boolean hasAnyPick() {
        return (pick3 != null && !pick3.isBlank()) || (pick4 != null && !pick4.isBlank());
    }
}
