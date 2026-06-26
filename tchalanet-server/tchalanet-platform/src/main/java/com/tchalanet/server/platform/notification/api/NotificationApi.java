package com.tchalanet.server.platform.notification.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchSearchQuery;
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

public interface NotificationApi {

    void createNotification(CreateNotificationRequest request);
    SendNotificationResult sendNotification(SendNotificationRequest request);
    void markRead(MarkNotificationReadRequest request);
    void archiveNotification(ArchiveNotificationRequest request);
    List<NotificationItemView> listNotifications(ListNotificationsRequest request);
    NotificationSummaryView getNotificationSummary(GetNotificationSummaryRequest request);
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
}
