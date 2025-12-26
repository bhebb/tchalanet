package com.tchalanet.server.core.draw.application.command.model;

import com.tchalanet.server.common.bus.Command;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

public record OpenDueDrawsCommand(
    @NotNull Instant now,
    @Positive int limit,
    @Positive int openHorizonHours,
    @Positive int openLagHours,
    boolean dryRun)
    implements Command<OpenDueDrawsResult> {}
