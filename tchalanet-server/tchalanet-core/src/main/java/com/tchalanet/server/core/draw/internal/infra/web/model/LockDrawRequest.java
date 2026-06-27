package com.tchalanet.server.core.draw.internal.infra.web.model;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.api.command.DrawLifecycleCommandLimits;
import jakarta.validation.constraints.Size;

import java.util.List;

public record LockDrawRequest(
    @Size(max = DrawLifecycleCommandLimits.MAX_DRAW_IDS) List<DrawId> drawIds,
    @Size(max = 255) String reason
) {}
