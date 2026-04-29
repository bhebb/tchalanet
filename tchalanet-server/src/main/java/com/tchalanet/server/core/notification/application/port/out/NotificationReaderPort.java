package com.tchalanet.server.core.notification.application.port.out;

import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.notification.application.query.model.NotificationDeliveryView;
import com.tchalanet.server.core.notification.application.query.model.NotificationItemView;
import com.tchalanet.server.core.notification.application.query.model.NotificationSummaryView;
import com.tchalanet.server.core.notification.domain.model.NotificationCategory;
import com.tchalanet.server.core.notification.domain.model.NotificationDeliveryStatus;
import com.tchalanet.server.core.notification.domain.model.NotificationKind;
import com.tchalanet.server.core.notification.domain.model.NotificationSeverity;
import com.tchalanet.server.core.notification.domain.model.NotificationStatus;
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
