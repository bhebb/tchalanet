package com.tchalanet.server.core.draw.application.command.model;

import com.tchalanet.server.common.bus.Command;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record OpenTodayDrawsCommand(
    @NotNull Instant now,
    LocalDate drawDate,
    @NotNull LocalTime defaultSalesOpenTime,
    @Positive int batchSize,
    boolean dryRun)
    implements Command<OpenDueDrawsResult> {}
