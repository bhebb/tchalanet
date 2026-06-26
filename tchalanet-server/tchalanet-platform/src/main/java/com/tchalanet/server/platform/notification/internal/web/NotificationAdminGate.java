package com.tchalanet.server.platform.notification.internal.web;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchSearchQuery;
import com.tchalanet.server.platform.notification.api.model.NotificationAudienceType;
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
import com.tchalanet.server.platform.notification.internal.service.NotificationService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class NotificationAdminGate {

  private final NotificationService notificationService;

  NotificationSummaryView summary(TchRequestContext context) {
    return notificationService.getNotificationSummary(
        new GetNotificationSummaryRequest(context.userId(), roleCode(context)));
  }

  TchPage<NotificationItemView> list(
      NotificationStatus status,
      NotificationCategory category,
      NotificationKind kind,
      NotificationSeverity severity,
      TchPageRequest pageRequest,
      TchRequestContext context) {
    return notificationService.listNotifications(
        new ListNotificationsRequest(
            context.userId(),
            roleCode(context),
            Optional.ofNullable(status),
            Optional.ofNullable(category),
            Optional.ofNullable(kind),
            Optional.ofNullable(severity),
            TchSearchQuery.empty(),
            pageRequest));
  }

  TchPage<NotificationItemView> listMy(
      NotificationStatus status,
      NotificationCategory category,
      NotificationKind kind,
      NotificationSeverity severity,
      TchSearchQuery search,
      TchPageRequest pageRequest,
      TchRequestContext context) {
    var userId = context.currentUserIdRequired();
    return notificationService.listMyNotifications(
        NotificationActorType.APP_USER,
        userId.value(),
        userId,
        roleCode(context),
        Optional.ofNullable(status),
        Optional.ofNullable(category),
        Optional.ofNullable(kind),
        Optional.ofNullable(severity),
        search,
        pageRequest);
  }

  NotificationUnreadCountView unreadCount(TchRequestContext context) {
    var userId = context.currentUserIdRequired();
    return notificationService.countUnread(
        NotificationActorType.APP_USER, userId.value(), userId, roleCode(context));
  }

  void createForTenant(CreateNotificationBody request, TchRequestContext context) {
    var tenantId = context.effectiveTenantIdRequired();
    var audienceType =
        request.audienceType() == null ? NotificationAudienceType.TENANT_ADMINS : request.audienceType();
    if (audienceType == NotificationAudienceType.PLATFORM_ADMINS
        || audienceType == NotificationAudienceType.ALL_APP_USERS) {
      throw ProblemRest.forbidden("notification.platform_audience_requires_platform_scope");
    }
    create(request, tenantId, audienceType, "ADMIN");
  }

  void createForPlatform(CreateNotificationBody request) {
    var audienceType =
        request.audienceType() == null ? NotificationAudienceType.PLATFORM_ADMINS : request.audienceType();
    if (audienceType == NotificationAudienceType.TENANT_ADMINS
        || audienceType == NotificationAudienceType.TENANT_APP_USERS
        || audienceType == NotificationAudienceType.TENANT_SELLER_TERMINALS) {
      throw ProblemRest.forbidden("notification.tenant_audience_requires_tenant_scope");
    }
    create(request, null, audienceType, "SUPER_ADMIN");
  }

  void createForTenantFromPlatform(CreateNotificationBody request, TenantId tenantId) {
    var audienceType =
        request.audienceType() == null ? NotificationAudienceType.TENANT_ADMINS : request.audienceType();
    if (audienceType == NotificationAudienceType.PLATFORM_ADMINS
        || audienceType == NotificationAudienceType.ALL_APP_USERS) {
      throw ProblemRest.forbidden("notification.platform_audience_requires_platform_scope");
    }
    create(request, tenantId, audienceType, "SUPER_ADMIN");
  }

  void markRead(NotificationId id, TchRequestContext context) {
    var userId = context.currentUserIdRequired();
    notificationService.markRead(id, NotificationActorType.APP_USER, userId.value());
  }

  void archive(NotificationId id, TchRequestContext context) {
    notificationService.archiveNotification(new ArchiveNotificationRequest(id, context.userId()));
  }

  void dismiss(NotificationId id, TchRequestContext context) {
    var userId = context.currentUserIdRequired();
    notificationService.dismiss(id, NotificationActorType.APP_USER, userId.value());
  }

  void markAllRead(TchRequestContext context) {
    var userId = context.currentUserIdRequired();
    notificationService.markAllRead(
        NotificationActorType.APP_USER, userId.value(), userId, roleCode(context));
  }

  Object publish(NotificationId id, NotificationLifecycleBody request, TchRequestContext context) {
    return notificationService.publish(id, context.currentUserIdRequired(), reason(request));
  }

  Object republish(NotificationId id, NotificationLifecycleBody request, TchRequestContext context) {
    return notificationService.republish(id, context.currentUserIdRequired(), reason(request));
  }

  Object replayRecipients(NotificationId id) {
    return notificationService.replayRecipients(id);
  }

  void cancel(NotificationId id, NotificationLifecycleBody request) {
    notificationService.cancel(id, reason(request));
  }

  Object purgeExpired(NotificationPurgeBody request) {
    return notificationService.purgeExpired(request != null && request.dryRun());
  }

  private void create(
      CreateNotificationBody request,
      TenantId tenantId,
      NotificationAudienceType audienceType,
      String sourceType) {
    notificationService.createNotification(
        new CreateNotificationRequest(
            tenantId,
            sourceType,
            request.sourceId(),
            request.dedupeKey(),
            audienceType,
            request.targets(),
            request.severity() == null ? NotificationSeverity.INFO : request.severity(),
            request.kind() == null ? NotificationKind.INFO : request.kind(),
            request.category() == null ? NotificationCategory.SYSTEM : request.category(),
            request.titleKey(),
            request.messageKey(),
            request.titleText(),
            request.messageText(),
            request.translations(),
            request.payload(),
            request.actionType(),
            request.actionUrl(),
            request.expiresAt(),
            request.channels()));
  }

  private static String roleCode(TchRequestContext context) {
    return context.currentRole() == null ? null : context.currentRole().name();
  }

  private static String reason(NotificationLifecycleBody request) {
    return request == null ? null : request.reason();
  }
}
