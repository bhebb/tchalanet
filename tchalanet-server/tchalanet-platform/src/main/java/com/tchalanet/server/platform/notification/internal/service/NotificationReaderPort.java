package com.tchalanet.server.platform.notification.internal.service;

import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationDeliveryStatus;
import com.tchalanet.server.platform.notification.api.model.NotificationDeliveryView;
import com.tchalanet.server.platform.notification.api.model.NotificationItemView;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import com.tchalanet.server.platform.notification.api.model.NotificationStatus;
import com.tchalanet.server.platform.notification.api.model.NotificationSummaryView;
import java.util.Optional;
import java.util.UUID;

public interface NotificationReaderPort {
  NotificationSummaryView summary(UserId userId, String roleCode);

  TchPage<NotificationItemView> list(
      UserId userId,
      String roleCode,
      Optional<NotificationStatus> status,
      Optional<NotificationCategory> category,
      Optional<NotificationKind> kind,
      Optional<NotificationSeverity> severity,
      TchPageRequest pageRequest);

  TchPage<NotificationDeliveryView> listDeliveries(
      Optional<UUID> notificationId,
      Optional<NotificationDeliveryStatus> status,
      TchPageRequest pageRequest);
}
