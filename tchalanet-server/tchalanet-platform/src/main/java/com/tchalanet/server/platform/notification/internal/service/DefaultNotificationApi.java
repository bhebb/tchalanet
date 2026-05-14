package com.tchalanet.server.platform.notification.internal.service;

import com.tchalanet.server.platform.notification.api.NotificationApi;
import com.tchalanet.server.platform.notification.api.model.request.ArchiveNotificationRequest;
import com.tchalanet.server.platform.notification.api.model.request.CreateNotificationRequest;
import com.tchalanet.server.platform.notification.api.model.request.GetNotificationSummaryRequest;
import com.tchalanet.server.platform.notification.api.model.request.ListNotificationsRequest;
import com.tchalanet.server.platform.notification.api.model.request.MarkNotificationReadRequest;
import com.tchalanet.server.platform.notification.api.model.view.NotificationItemView;
import com.tchalanet.server.platform.notification.api.model.view.NotificationSummaryView;
import com.tchalanet.server.platform.notification.api.model.request.SendNotificationRequest;
import com.tchalanet.server.platform.notification.api.model.SendNotificationResult;
import java.util.List;
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
}
