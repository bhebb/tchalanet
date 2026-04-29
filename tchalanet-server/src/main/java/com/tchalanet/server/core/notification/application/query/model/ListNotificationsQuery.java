package com.tchalanet.server.core.notification.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.notification.domain.model.NotificationCategory;
import com.tchalanet.server.core.notification.domain.model.NotificationKind;
import com.tchalanet.server.core.notification.domain.model.NotificationSeverity;
import com.tchalanet.server.core.notification.domain.model.NotificationStatus;
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
