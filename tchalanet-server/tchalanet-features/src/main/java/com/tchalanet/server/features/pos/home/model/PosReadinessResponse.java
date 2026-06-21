package com.tchalanet.server.features.pos.home.model;

import java.util.List;

public record PosReadinessResponse(
    boolean ready,
    PosAttentionLevel attentionLevel,
    List<PosBadge> badges,
    List<PosNotification> notifications,
    List<PosReadinessBlocker> blockers
) {
    public PosReadinessResponse {
        badges = badges == null ? List.of() : List.copyOf(badges);
        notifications = notifications == null ? List.of() : List.copyOf(notifications);
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
    }
}
