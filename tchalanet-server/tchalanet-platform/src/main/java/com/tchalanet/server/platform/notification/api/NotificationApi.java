package com.tchalanet.server.platform.notification.api;

import java.util.List;

import com.tchalanet.server.platform.notification.api.model.request.ArchiveNotificationRequest;
import com.tchalanet.server.platform.notification.api.model.request.CreateNotificationRequest;
import com.tchalanet.server.platform.notification.api.model.request.GetNotificationSummaryRequest;
import com.tchalanet.server.platform.notification.api.model.request.ListNotificationsRequest;
import com.tchalanet.server.platform.notification.api.model.request.MarkNotificationReadRequest;
import com.tchalanet.server.platform.notification.api.model.view.NotificationItemView;
import com.tchalanet.server.platform.notification.api.model.view.NotificationSummaryView;
import com.tchalanet.server.platform.notification.api.model.request.SendNotificationRequest;
import com.tchalanet.server.platform.notification.api.model.SendNotificationResult;

public interface NotificationApi {

    void createNotification(CreateNotificationRequest request);
    SendNotificationResult sendNotification(SendNotificationRequest request);
    void markRead(MarkNotificationReadRequest request);
    void archiveNotification(ArchiveNotificationRequest request);
    List<NotificationItemView> listNotifications(ListNotificationsRequest request);
    NotificationSummaryView getNotificationSummary(GetNotificationSummaryRequest request);
}
