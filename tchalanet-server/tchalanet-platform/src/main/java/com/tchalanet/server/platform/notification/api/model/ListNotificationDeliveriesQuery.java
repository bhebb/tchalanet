package com.tchalanet.server.platform.notification.api.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import java.util.Optional;

public record ListNotificationDeliveriesQuery(
    Optional<NotificationId> notificationId,
    Optional<NotificationDeliveryStatus> status,
    TchPageRequest pageRequest)
    implements Query<TchPage<NotificationDeliveryView>> {}
