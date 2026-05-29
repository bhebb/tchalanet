package com.tchalanet.server.core.draw.api.command;

import com.tchalanet.server.common.bus.Command;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.time.LocalDate;

public record OpenTodayDrawsCommand(
    @NotNull Instant now,
    LocalDate drawDate,
    @Positive int batchSize,
    boolean dryRun)
    implements Command<OpenDueDrawsResult> {}
