package com.tchalanet.server.core.draw.application.command.model;

import com.tchalanet.server.common.bus.Command;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record GenerateDrawsForRangeCommand(
    @NotNull UUID tenantId,
    @NotNull LocalDate from,
    @NotNull LocalDate to,
    boolean dryRun,
    boolean force) implements Command<GenerateDrawsForRangeResult> {
}
