package com.tchalanet.server.features.ops.drawresult.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

@Schema(description = "Request to override an existing draw result for a slot/date.")
public record OverrideDrawResultRequest(
    @NotBlank
    @Schema(description = "Result slot key.", example = "NY_MID")
    String slotKey,

    @NotNull
    @Schema(description = "Draw calendar date.", example = "2026-05-02")
    LocalDate drawDate,

    @NotBlank
    @Pattern(regexp = "\\d{3}", message = "lot1 must contain exactly 3 digits")
    @Schema(description = "Haiti lot 1 result. Mapped server-side to pick3.", example = "123")
    String lot1,

    @NotBlank
    @Pattern(regexp = "\\d{2}", message = "lot2 must contain exactly 2 digits")
    @Schema(description = "Haiti lot 2 result. Mapped server-side to pick4 prefix.", example = "45")
    String lot2,

    @NotBlank
    @Pattern(regexp = "\\d{2}", message = "lot3 must contain exactly 2 digits")
    @Schema(description = "Haiti lot 3 result. Mapped server-side to pick4 suffix.", example = "67")
    String lot3,

    @NotBlank
    @Schema(description = "Operational reason for override.", example = "Official provider correction")
    String reason,

    @Schema(description = "Whether override may replace protected result states.", example = "true")
    boolean force
) {}
