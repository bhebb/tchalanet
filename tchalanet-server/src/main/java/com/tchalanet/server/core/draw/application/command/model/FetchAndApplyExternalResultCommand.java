package com.tchalanet.server.core.draw.application.command.model;


import java.time.Instant;
import java.util.UUID;

public record FetchAndApplyExternalResultCommand(
    UUID drawId,
    Instant executedAt,
    boolean force
) {
    public FetchAndApplyExternalResultCommand {
        if (drawId == null) throw new IllegalArgumentException("drawId required");
        if (executedAt == null) throw new IllegalArgumentException("executedAt required");
    }

    public static FetchAndApplyExternalResultCommand normal(UUID drawId, Instant now) {
        return new FetchAndApplyExternalResultCommand(drawId, now, false);
    }

    public static FetchAndApplyExternalResultCommand forced(UUID drawId, Instant now) {
        return new FetchAndApplyExternalResultCommand(drawId, now, true);
    }
}
