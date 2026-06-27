package com.tchalanet.server.features.ops.draw;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Request to aplly external lottery results into global draw_result storage.")
public record ApplyExternalResultsRequest(
    @Schema(description = "Optional tenant codes to target. Empty means all active tenants up to the interactive launch limit.")
    List<String> tenantCodes,

    @Schema(
        description = "Base calendar date used for the fetch window. Defaults server-side if omitted.",
        example = "2026-05-02")
    LocalDate baseDate,

    @Min(0)
    @Schema(description = "Number of days before baseDate to include.", example = "1", defaultValue = "0")
    Integer daysBack,

    @Size(max = 50)
    @Schema(
        description = "Optional slot keys to fetch. Empty means all eligible active result slots.",
        example = "[\"NY_MID\", \"FL_EVE\"]")
    List<String> slotKeys,

    @Schema(description = "Force overwrite when allowed by command rules.", example = "false")
    boolean force,

    @Schema(description = "Validate and report what would happen without writing.", example = "true")
    boolean dryRun,

    @Min(1)
    @Schema(description = "Safety cap for processed slot/date combinations.", example = "200")
    Integer maxSlots,

    @Schema(description = "Required when force=true.", example = "Provider correction verified by ops")
    String reason
) {
    @AssertTrue(message = "reason is required when force is true")
    public boolean isReasonValidForForce() {
        return !force || (reason != null && !reason.isBlank());
    }
}
