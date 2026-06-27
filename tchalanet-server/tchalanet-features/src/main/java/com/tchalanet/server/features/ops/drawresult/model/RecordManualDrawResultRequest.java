package com.tchalanet.server.features.ops.drawresult.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

@Schema(description = "Request to record a manual draw result when provider fetch is unavailable.")
public record RecordManualDrawResultRequest(
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

    @Schema(description = "Force manual record when allowed by command rules.", example = "false")
    boolean force,

    @NotBlank
    @Schema(description = "Operational reason for manual entry.", example = "Manual correction approved by platform ops")
    String reason
) {}
