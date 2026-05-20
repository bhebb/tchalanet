package com.tchalanet.server.core.draw.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CorrectAppliedDrawResultCommand(
    @NotNull DrawId drawId,
    DrawResultId correctedDrawResultId,
    @Size(max = 255) String reason,
    String idempotencyKey,
    boolean force
) implements Command<Void> {
}
