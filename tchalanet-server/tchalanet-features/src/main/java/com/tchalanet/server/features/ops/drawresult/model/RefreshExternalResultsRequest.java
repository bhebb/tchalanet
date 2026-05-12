package com.tchalanet.server.features.ops.drawresult.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Request to fetch external results then apply them to tenant draws.")
public record RefreshExternalResultsRequest(
    @NotBlank
    @Schema(description = "Tenant UUID. Required because apply is tenant-scoped.")
    String tenantId,

    @Schema(example = "2026-05-02")
    LocalDate baseDate,

    @Min(0)
    @Schema(example = "1", defaultValue = "0")
    Integer daysBack,

    @Size(max = 50)
    @Schema(example = "[\"NY_MID\", \"FL_EVE\"]")
    List<String> slotKeys,

    @Schema(example = "false")
    boolean force,

    @Schema(example = "true")
    boolean dryRun,

    @Min(1)
    @Schema(example = "200")
    Integer maxSlots,

    @Schema(description = "Required when force=true.")
    String reason
) {
    @AssertTrue(message = "reason is required when force is true")
    public boolean isReasonValidForForce() {
        return !force || (reason != null && !reason.isBlank());
    }
}
