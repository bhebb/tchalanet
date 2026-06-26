package com.tchalanet.server.platform.notification.internal.service;

import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchSearchQuery;
import com.tchalanet.server.platform.notification.api.model.NotificationActorType;
import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.view.NotificationItemView;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import com.tchalanet.server.platform.notification.api.model.NotificationStatus;
import com.tchalanet.server.platform.notification.api.model.view.NotificationUnreadCountView;
import com.tchalanet.server.platform.notification.api.model.view.NotificationSummaryView;
import java.util.Optional;
import java.util.UUID;

public interface NotificationReader {
  NotificationSummaryView summary(UserId userId, String roleCode);

  NotificationSummaryView summaryForTerminal(SellerTerminalId sellerTerminalId);

  TchPage<NotificationItemView> list(
      UserId userId,
      String roleCode,
      Optional<NotificationStatus> status,
      Optional<NotificationCategory> category,
      Optional<NotificationKind> kind,
      Optional<NotificationSeverity> severity,
      TchSearchQuery search,
      TchPageRequest pageRequest);

  TchPage<NotificationItemView> listForTerminal(
      SellerTerminalId sellerTerminalId,
      Optional<NotificationStatus> status,
      Optional<NotificationCategory> category,
      Optional<NotificationKind> kind,
      Optional<NotificationSeverity> severity,
      TchSearchQuery search,
      TchPageRequest pageRequest);

  TchPage<NotificationItemView> listMyNotifications(
      NotificationActorType actorType,
      UUID actorId,
      UserId userId,
      String roleCode,
      Optional<NotificationStatus> status,
      Optional<NotificationCategory> category,
      Optional<NotificationKind> kind,
      Optional<NotificationSeverity> severity,
      TchSearchQuery search,
      TchPageRequest pageRequest);

  NotificationUnreadCountView countUnread(
      NotificationActorType actorType,
      UUID actorId,
      UserId userId,
      String roleCode);

  void markRead(NotificationId notificationId, NotificationActorType actorType, UUID actorId);

  void dismiss(NotificationId notificationId, NotificationActorType actorType, UUID actorId);

  void markAllRead(NotificationActorType actorType, UUID actorId, UserId userId, String roleCode);
}
