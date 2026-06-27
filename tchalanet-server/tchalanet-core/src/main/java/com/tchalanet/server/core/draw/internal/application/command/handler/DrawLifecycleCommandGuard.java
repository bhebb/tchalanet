package com.tchalanet.server.core.draw.internal.application.command.handler;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.api.command.DrawLifecycleCommandLimits;

import java.util.List;
import java.util.Objects;

final class DrawLifecycleCommandGuard {

    private DrawLifecycleCommandGuard() {
    }

    static List<DrawId> requireDrawIds(List<DrawId> drawIds) {
        if (drawIds == null || drawIds.isEmpty()) {
            throw new IllegalArgumentException("drawIds is required");
        }
        var normalized = drawIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("drawIds is required");
        }
        if (normalized.size() > DrawLifecycleCommandLimits.MAX_DRAW_IDS) {
            throw new IllegalArgumentException("drawIds cannot contain more than " + DrawLifecycleCommandLimits.MAX_DRAW_IDS + " items");
        }
        return normalized;
    }
}
