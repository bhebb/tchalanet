package com.tchalanet.server.platform.notification.internal.service;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.NotificationDeliveryId;
import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.platform.notification.api.model.NotificationAudienceType;
import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationChannel;
import com.tchalanet.server.platform.notification.api.model.NotificationDeliveryStatus;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationRecipient;
import com.tchalanet.server.platform.notification.api.model.NotificationStatus;
import com.tchalanet.server.platform.notification.api.model.SendNotificationResult;
import com.tchalanet.server.platform.notification.api.model.request.ArchiveNotificationRequest;
import com.tchalanet.server.platform.notification.api.model.request.ArchiveNotificationsRequest;
import com.tchalanet.server.platform.notification.api.model.request.CreateNotificationRequest;
import com.tchalanet.server.platform.notification.api.model.request.ExpireNotificationsRequest;
import com.tchalanet.server.platform.notification.api.model.request.GetNotificationSummaryRequest;
import com.tchalanet.server.platform.notification.api.model.request.ListNotificationDeliveriesRequest;
import com.tchalanet.server.platform.notification.api.model.request.ListNotificationsRequest;
import com.tchalanet.server.platform.notification.api.model.request.MarkNotificationReadRequest;
import com.tchalanet.server.platform.notification.api.model.request.MarkNotificationsReadRequest;
import com.tchalanet.server.platform.notification.api.model.request.ScheduleNotificationDeliveryRequest;
import com.tchalanet.server.platform.notification.api.model.request.SendNotificationRequest;
import com.tchalanet.server.platform.notification.api.model.view.NotificationDeliveryView;
import com.tchalanet.server.platform.notification.api.model.view.NotificationItemView;
import com.tchalanet.server.platform.notification.api.model.view.NotificationSummaryView;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Application service for the notification capability. */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

  private final Clock clock;
  private final IdGenerator idGenerator;
  private final JsonUtils jsonUtils;
  private final NotificationWriterPort notificationWriter;
  private final NotificationDeliveryWriterPort deliveryWriter;
  private final NotificationReaderPort reader;
  private final NotificationTemplateRenderer templateRenderer;
  private final NotificationPolicy notificationPolicy;

  @TchTx
  public void createNotification(CreateNotificationRequest request) {
    if (request.dedupeKey() != null && !request.dedupeKey().isBlank()) {
      var existing = notificationWriter.findByDedupeKey(request.dedupeKey());
      if (existing.isPresent()) {
        return;
      }
    }

    var now = clock.instant();
    var notificationId = NotificationId.of(idGenerator.newUuid());
    var rendered =
        templateRenderer.render(
            request.tenantId(),
            firstNonBlank(request.titleKey(), request.messageKey()),
            null,
            request.titleText(),
            request.messageText(),
            request.payload());
    var notification =
        new Notification(
            notificationId,
            request.tenantId(),
            request.sourceType(),
            request.sourceId(),
            request.dedupeKey(),
            request.audienceType(),
            request.audienceValue(),
            request.severity(),
            request.kind(),
            request.category(),
            request.titleKey(),
            request.messageKey(),
            rendered.title(),
            rendered.message(),
            request.payload(),
            new NotificationAction(request.actionType(), request.actionUrl()),
            NotificationStatus.UNREAD,
            null,
            null,
            request.expiresAt(),
            now,
            now);

    var saved = notificationWriter.save(notification);
    scheduleDeliveries(saved, request.channels(), now);
  }

  @TchTx
  public SendNotificationResult sendNotification(SendNotificationRequest request) {
    log.debug(
        "Handling SendNotificationRequest: type={}, severity={}, recipients={}",
        request.type(),
        request.severity(),
        request.recipients().size());

    notificationPolicy.validateRecipients(request.recipients());

    var idempotencyKey =
        request.idempotencyKey() != null
            ? request.idempotencyKey()
            : generateIdempotencyKey(request);

    for (var recipient : request.recipients()) {
      try {
        createInAppNotification(request, recipient, idempotencyKey);
      } catch (Exception e) {
        log.error(
            "Failed to send notification to recipient: channel={}, error={}",
            recipient.channel(),
            e.getMessage(),
            e);
        return SendNotificationResult.failed(
            "Failed to send notification: " + e.getMessage(), idempotencyKey);
      }
    }

    log.info(
        "Notification sent successfully: type={}, idempotencyKey={}",
        request.type(),
        idempotencyKey);

    return SendNotificationResult.accepted(idempotencyKey);
  }

  @TchTx
  public void markRead(MarkNotificationReadRequest request) {
    notificationWriter.markRead(request.notificationId(), clock.instant());
  }

  @TchTx
  public void markRead(MarkNotificationsReadRequest request) {
    notificationWriter.markRead(request.notificationIds(), clock.instant());
  }

  @TchTx
  public void archiveNotification(ArchiveNotificationRequest request) {
    notificationWriter.archive(request.notificationId(), clock.instant());
  }

  @TchTx
  public void archiveNotifications(ArchiveNotificationsRequest request) {
    notificationWriter.archive(request.notificationIds(), clock.instant());
  }

  @TchTx
  public Integer expireNotifications(ExpireNotificationsRequest request) {
    var now = request.now() == null ? clock.instant() : request.now();
    return notificationWriter.expire(now);
  }

  @TchTx
  public NotificationDeliveryId scheduleDelivery(ScheduleNotificationDeliveryRequest request) {
    var now = clock.instant();
    var id = NotificationDeliveryId.of(idGenerator.newUuid());
    var saved =
        deliveryWriter.save(
            new NotificationDelivery(
                id,
                null,
                request.notificationId(),
                request.channel(),
                request.recipient(),
                NotificationDeliveryStatus.PENDING,
                0,
                request.nextAttemptAt() == null ? now : request.nextAttemptAt(),
                null,
                null,
                null,
                null,
                null,
                request.payload(),
                now,
                now));
    return saved.id();
  }

  public NotificationSummaryView getNotificationSummary(GetNotificationSummaryRequest request) {
    return reader.summary(request.userId(), request.roleCode());
  }

  public TchPage<NotificationItemView> listNotifications(ListNotificationsRequest request) {
    return reader.list(
        request.userId(),
        request.roleCode(),
        request.status(),
        request.category(),
        request.kind(),
        request.severity(),
        request.pageRequest());
  }

  public TchPage<NotificationDeliveryView> listDeliveries(
      ListNotificationDeliveriesRequest request) {
    return reader.listDeliveries(
        request.notificationId().map(id -> id.value()), request.status(), request.pageRequest());
  }

  private String firstNonBlank(String first, String second) {
    if (first != null && !first.isBlank()) {
      return first;
    }
    return second;
  }

  private void scheduleDeliveries(
      Notification notification, Set<NotificationChannel> channels, Instant now) {
    var requested =
        channels == null || channels.isEmpty() ? Set.of(NotificationChannel.WEB) : channels;
    for (var channel : requested) {
      deliveryWriter.save(
          new NotificationDelivery(
              NotificationDeliveryId.of(idGenerator.newUuid()),
              notification.tenantId(),
              notification.id(),
              channel,
              notification.audienceValue(),
              NotificationDeliveryStatus.PENDING,
              0,
              now,
              null,
              null,
              null,
              null,
              null,
              notification.payload(),
              now,
              now));
    }
  }

  private void createInAppNotification(
      SendNotificationRequest request, NotificationRecipient recipient, String idempotencyKey) {
    var now = clock.instant();
    var notificationId = NotificationId.of(idGenerator.newUuid());
    var tenantId = recipient.tenantId();
    var audienceType = audienceType(recipient);
    var audienceValue = audienceValue(recipient, audienceType);
    var dedupeKey =
        idempotencyKey + ":" + recipient.channel() + ":" + audienceType + ":" + audienceValue;

    if (notificationWriter.findByDedupeKey(dedupeKey).isPresent()) {
      return;
    }

    var notification =
        new Notification(
            notificationId,
            tenantId,
            "SendNotificationRequest",
            request.type().name(),
            dedupeKey,
            audienceType,
            audienceValue,
            request.severity(),
            NotificationKind.INFO,
            NotificationCategory.SYSTEM,
            request.type().name(),
            request.type().name(),
            request.title(),
            request.message(),
            jsonUtils.toJsonNode(request.context()),
            new NotificationAction(null, null),
            NotificationStatus.UNREAD,
            null,
            null,
            null,
            now,
            now);

    var saved = notificationWriter.save(notification);
    deliveryWriter.save(
        new NotificationDelivery(
            NotificationDeliveryId.of(idGenerator.newUuid()),
            tenantId,
            saved.id(),
            normalizeChannel(recipient.channel()),
            audienceValue,
            NotificationDeliveryStatus.PENDING,
            0,
            now,
            null,
            null,
            null,
            null,
            null,
            notification.payload(),
            now,
            now));
  }

  private NotificationChannel normalizeChannel(NotificationChannel channel) {
    return channel == null ? NotificationChannel.WEB : channel;
  }

  private NotificationAudienceType audienceType(NotificationRecipient recipient) {
    if (recipient.userId() != null) {
      return NotificationAudienceType.USER;
    }
    if (recipient.tenantId() != null) {
      return NotificationAudienceType.TENANT;
    }
    return NotificationAudienceType.PLATFORM;
  }

  private String audienceValue(NotificationRecipient recipient, NotificationAudienceType type) {
    return switch (type) {
      case USER -> recipient.userId().value().toString();
      case TENANT -> recipient.tenantId().value().toString();
      case PLATFORM -> "platform";
      case ROLE, OUTLET, TERMINAL -> "unsupported";
    };
  }

  private String generateIdempotencyKey(SendNotificationRequest request) {
    return request.type().name() + "_" + UUID.randomUUID();
  }
}
