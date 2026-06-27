package com.tchalanet.server.core.draw.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.DrawId;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ArchiveDrawCommand(
    @NotEmpty @Size(max = DrawLifecycleCommandLimits.MAX_DRAW_IDS) List<@NotNull DrawId> drawIds,
    @Size(max = 255) String reason,
    boolean force
) implements Command<Void> {}
