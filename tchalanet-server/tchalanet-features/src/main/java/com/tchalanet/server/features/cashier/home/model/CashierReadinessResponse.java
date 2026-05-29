package com.tchalanet.server.features.cashier.home.model;

import java.util.List;

public record CashierReadinessResponse(
    boolean ready,
    CashierAttentionLevel attentionLevel,
    List<CashierBadge> badges,
    List<CashierNotification> notifications,
    List<CashierReadinessBlocker> blockers
) {
    public CashierReadinessResponse {
        badges = badges == null ? List.of() : List.copyOf(badges);
        notifications = notifications == null ? List.of() : List.copyOf(notifications);
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
    }
}
