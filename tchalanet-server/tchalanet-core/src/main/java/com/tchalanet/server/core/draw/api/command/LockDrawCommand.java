package com.tchalanet.server.core.draw.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.DrawId;
// import com.tchalanet.server.common.types.id.TenantId; // Removed
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LockDrawCommand(
    @NotNull DrawId drawId,
    @Size(max = 255) String reason
) implements Command<Void> {}
