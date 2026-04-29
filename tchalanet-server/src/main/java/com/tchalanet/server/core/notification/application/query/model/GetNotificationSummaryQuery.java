package com.tchalanet.server.core.notification.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.UserId;

public record GetNotificationSummaryQuery(UserId userId, String roleCode)
    implements Query<NotificationSummaryView> {}
