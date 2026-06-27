package com.tchalanet.server.core.draw.internal.infra.web.model;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.api.command.DrawLifecycleCommandLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CancelDrawRequest(
    @Size(max = DrawLifecycleCommandLimits.MAX_DRAW_IDS) List<DrawId> drawIds,
    @NotBlank @Size(max = 96) String reasonCode,
    @Size(max = 255) String reasonLabel,
    boolean force
) {}
