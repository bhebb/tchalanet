package com.tchalanet.server.core.draw.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.domain.model.DrawStatus;
import com.tchalanet.server.core.draw.domain.model.DrawSummary;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record OverrideDrawCommand(
    @NotNull DrawId drawId,
    DrawStatus status,
    Instant scheduledAt,
    Instant cutoffAt,
    @Size(max = 255) String reason,
    boolean force
) implements Command<Void> {
}
