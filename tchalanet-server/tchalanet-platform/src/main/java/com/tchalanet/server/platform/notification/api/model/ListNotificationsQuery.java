package com.tchalanet.server.platform.notification.api.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import java.util.Optional;

public record ListNotificationsQuery(
    UserId userId,
    String roleCode,
    Optional<NotificationStatus> status,
    Optional<NotificationCategory> category,
    Optional<NotificationKind> kind,
    Optional<NotificationSeverity> severity,
    TchPageRequest pageRequest)
    implements Query<TchPage<NotificationItemView>> {}
