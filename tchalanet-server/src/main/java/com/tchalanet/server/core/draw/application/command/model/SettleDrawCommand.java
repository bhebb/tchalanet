package com.tchalanet.server.core.draw.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.DrawId;
// import com.tchalanet.server.common.types.id.TenantId; // Removed
import jakarta.validation.constraints.NotNull; // Added for drawId
import jakarta.validation.constraints.Size; // Added for reason

public record SettleDrawCommand(
    @NotNull DrawId drawId,
    @Size(max = 255) String reason, // Added as per controller usage
    boolean force // Added as per controller usage
) implements Command<Void> {}
