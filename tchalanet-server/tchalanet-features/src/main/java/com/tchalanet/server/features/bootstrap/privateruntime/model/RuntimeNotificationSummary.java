package com.tchalanet.server.features.bootstrap;

import com.tchalanet.server.platform.notification.api.model.view.NotificationSummaryView;

public record RuntimeNotificationSummary(
    long unreadCount,
    long criticalCount
) {
    public static RuntimeNotificationSummary empty() {
        return new RuntimeNotificationSummary(0L, 0L);
    }

    public static RuntimeNotificationSummary from(NotificationSummaryView view) {
        return new RuntimeNotificationSummary(view.unreadCount(), view.criticalCount());
    }
}
