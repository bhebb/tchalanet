package com.tchalanet.server.core.draw.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.DrawId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CancelDrawCommand(
    @NotNull DrawId drawId,
    @NotBlank @Size(max = 96) String reasonCode,
    @Size(max = 255) String reasonLabel,
    boolean force
) implements Command<Void> {}
