package com.tchalanet.server.platform.notification.api;

import java.util.List;

import com.tchalanet.server.platform.notification.api.model.ArchiveNotificationCommand;
import com.tchalanet.server.platform.notification.api.model.CreateNotificationCommand;
import com.tchalanet.server.platform.notification.api.model.GetNotificationSummaryQuery;
import com.tchalanet.server.platform.notification.api.model.ListNotificationsQuery;
import com.tchalanet.server.platform.notification.api.model.MarkNotificationReadCommand;
import com.tchalanet.server.platform.notification.api.model.NotificationItemView;
import com.tchalanet.server.platform.notification.api.model.NotificationSummaryView;
import com.tchalanet.server.platform.notification.api.model.SendNotificationCommand;
import com.tchalanet.server.platform.notification.api.model.SendNotificationResult;

public interface NotificationApi {

    void createNotification(CreateNotificationCommand request);
    SendNotificationResult sendNotification(SendNotificationCommand request);
    void markRead(MarkNotificationReadCommand request);
    void archiveNotification(ArchiveNotificationCommand request);
    List<NotificationItemView> listNotifications(ListNotificationsQuery request);
    NotificationSummaryView getNotificationSummary(GetNotificationSummaryQuery request);
}
