package com.tchalanet.server.platform.notification.internal.service;

import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchSearchQuery;
import com.tchalanet.server.platform.notification.api.NotificationApi;
import com.tchalanet.server.platform.notification.api.model.NotificationActorType;
import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import com.tchalanet.server.platform.notification.api.model.NotificationStatus;
import com.tchalanet.server.platform.notification.api.model.request.ArchiveNotificationRequest;
import com.tchalanet.server.platform.notification.api.model.request.CreateNotificationRequest;
import com.tchalanet.server.platform.notification.api.model.request.GetNotificationSummaryRequest;
import com.tchalanet.server.platform.notification.api.model.request.ListNotificationsRequest;
import com.tchalanet.server.platform.notification.api.model.request.MarkNotificationReadRequest;
import com.tchalanet.server.platform.notification.api.model.view.NotificationItemView;
import com.tchalanet.server.platform.notification.api.model.view.NotificationSummaryView;
import com.tchalanet.server.platform.notification.api.model.view.NotificationUnreadCountView;
import com.tchalanet.server.platform.notification.api.model.request.SendNotificationRequest;
import com.tchalanet.server.platform.notification.api.model.SendNotificationResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class DefaultNotificationApi implements NotificationApi {

  private final NotificationService notificationService;

  @Override
  public void createNotification(CreateNotificationRequest request) {
    notificationService.createNotification(request);
  }

  @Override
  public SendNotificationResult sendNotification(SendNotificationRequest request) {
    return notificationService.sendNotification(request);
  }

  @Override
  public void markRead(MarkNotificationReadRequest request) {
    notificationService.markRead(request);
  }

  @Override
  public void archiveNotification(ArchiveNotificationRequest request) {
    notificationService.archiveNotification(request);
  }

  @Override
  public List<NotificationItemView> listNotifications(ListNotificationsRequest request) {
    return notificationService.listNotifications(request).items();
  }

  @Override
  public NotificationSummaryView getNotificationSummary(GetNotificationSummaryRequest request) {
    return notificationService.getNotificationSummary(request);
  }

  @Override
  public TchPage<NotificationItemView> listMyNotifications(
      NotificationActorType actorType,
      UUID actorId,
      UserId userId,
      String roleCode,
      Optional<NotificationStatus> status,
      Optional<NotificationCategory> category,
      Optional<NotificationKind> kind,
      Optional<NotificationSeverity> severity,
      TchSearchQuery search,
      TchPageRequest pageRequest) {
    return notificationService.listMyNotifications(
        actorType,
        actorId,
        userId,
        roleCode,
        status,
        category,
        kind,
        severity,
        search,
        pageRequest);
  }

  @Override
  public NotificationUnreadCountView countUnread(
      NotificationActorType actorType,
      UUID actorId,
      UserId userId,
      String roleCode) {
    return notificationService.countUnread(actorType, actorId, userId, roleCode);
  }
}
