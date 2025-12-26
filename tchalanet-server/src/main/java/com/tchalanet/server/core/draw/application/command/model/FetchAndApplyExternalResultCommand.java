package com.tchalanet.server.core.draw.application.command.model;
import com.tchalanet.server.common.types.id.DrawId;


import java.time.Instant;
import java.util.UUID;

public record FetchAndApplyExternalResultCommand(
    DrawId drawId,
    Instant executedAt,
    boolean force
) {
    public FetchAndApplyExternalResultCommand {
        if (drawId == null) throw new IllegalArgumentException("drawId required");
        if (executedAt == null) throw new IllegalArgumentException("executedAt required");
    }

    public static FetchAndApplyExternalResultCommand normal(DrawId drawId, Instant now) {
        return new FetchAndApplyExternalResultCommand(drawId, now, false);
    }

    public static FetchAndApplyExternalResultCommand forced(DrawId drawId, Instant now) {
        return new FetchAndApplyExternalResultCommand(drawId, now, true);
    }
}
