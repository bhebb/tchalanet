package com.tchalanet.server.core.notification.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.notification.domain.model.NotificationDeliveryStatus;
import java.util.Optional;

public record ListNotificationDeliveriesQuery(
    Optional<NotificationId> notificationId,
    Optional<NotificationDeliveryStatus> status,
    TchPageRequest pageRequest)
    implements Query<TchPage<NotificationDeliveryView>> {}
