package com.tchalanet.server.features.bootstrap;

import com.tchalanet.server.platform.notification.api.model.view.NotificationSummaryView;

public record RuntimeNotificationSummary(
    long unreadCount,
    long criticalCount
) {
    public static com.tchalanet.server.features.bootstrap.publicruntime.model.RuntimeNotificationSummary empty() {
        return new com.tchalanet.server.features.bootstrap.publicruntime.model.RuntimeNotificationSummary(0L, 0L);
    }

    public static com.tchalanet.server.features.bootstrap.publicruntime.model.RuntimeNotificationSummary from(NotificationSummaryView view) {
        return new com.tchalanet.server.features.bootstrap.publicruntime.model.RuntimeNotificationSummary(view.unreadCount(), view.criticalCount());
    }
}
