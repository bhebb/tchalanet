package com.tchalanet.server.platform.notification.api.model.request;
import com.tchalanet.server.platform.notification.api.model.NotificationDeliveryStatus;

import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import java.util.Optional;

public record ListNotificationDeliveriesRequest(
    Optional<NotificationId> notificationId,
    Optional<NotificationDeliveryStatus> status,
    TchPageRequest pageRequest) {}
