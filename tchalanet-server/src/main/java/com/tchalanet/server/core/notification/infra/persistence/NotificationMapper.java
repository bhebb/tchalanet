package com.tchalanet.server.core.notification.infra.persistence;

import com.tchalanet.server.common.types.id.NotificationDeliveryId;
import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.notification.application.query.model.NotificationActionView;
import com.tchalanet.server.core.notification.application.query.model.NotificationDeliveryView;
import com.tchalanet.server.core.notification.application.query.model.NotificationItemView;
import com.tchalanet.server.core.notification.domain.model.Notification;
import com.tchalanet.server.core.notification.domain.model.NotificationAction;
import com.tchalanet.server.core.notification.domain.model.NotificationDelivery;

final class NotificationMapper {

  private NotificationMapper() {}

  static NotificationJpaEntity toEntity(Notification notification, NotificationJpaEntity entity) {
    var target = entity == null ? new NotificationJpaEntity() : entity;
    target.setId(notification.id().value());
    target.setTenantId(notification.tenantId() == null ? null : notification.tenantId().value());
    target.setSourceType(notification.sourceType());
    target.setSourceId(notification.sourceId());
    target.setDedupeKey(notification.dedupeKey());
    target.setAudienceType(notification.audienceType());
    target.setAudienceValue(notification.audienceValue());
    target.setSeverity(notification.severity());
    target.setKind(notification.kind());
    target.setCategory(notification.category());
    target.setTitleKey(notification.titleKey());
    target.setMessageKey(notification.messageKey());
    target.setTitleText(notification.titleText());
    target.setMessageText(notification.messageText());
    target.setPayload(notification.payload());
    target.setActionType(notification.action() == null ? null : notification.action().type());
    target.setActionUrl(notification.action() == null ? null : notification.action().url());
    target.setStatus(notification.status());
    target.setReadAt(notification.readAt());
    target.setArchivedAt(notification.archivedAt());
    target.setExpiresAt(notification.expiresAt());
    target.setCreatedAt(notification.createdAt());
    target.setUpdatedAt(notification.updatedAt());
    return target;
  }

  static Notification toDomain(NotificationJpaEntity entity) {
    return new Notification(
        NotificationId.of(entity.getId()),
        TenantId.nullableOf(entity.getTenantId()),
        entity.getSourceType(),
        entity.getSourceId(),
        entity.getDedupeKey(),
        entity.getAudienceType(),
        entity.getAudienceValue(),
        entity.getSeverity(),
        entity.getKind(),
        entity.getCategory(),
        entity.getTitleKey(),
        entity.getMessageKey(),
        entity.getTitleText(),
        entity.getMessageText(),
        entity.getPayload(),
        new NotificationAction(entity.getActionType(), entity.getActionUrl()),
        entity.getStatus(),
        entity.getReadAt(),
        entity.getArchivedAt(),
        entity.getExpiresAt(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  static NotificationItemView toItemView(NotificationJpaEntity entity) {
    return new NotificationItemView(
        NotificationId.of(entity.getId()),
        entity.getSeverity(),
        entity.getKind(),
        entity.getCategory(),
        entity.getTitleKey(),
        entity.getMessageKey(),
        entity.getTitleText(),
        entity.getMessageText(),
        entity.getPayload(),
        new NotificationActionView(entity.getActionType(), entity.getActionUrl()),
        entity.getStatus(),
        entity.getReadAt(),
        entity.getArchivedAt(),
        entity.getExpiresAt(),
        entity.getCreatedAt());
  }

  static NotificationDeliveryJpaEntity toEntity(
      NotificationDelivery delivery, NotificationDeliveryJpaEntity entity) {
    var target = entity == null ? new NotificationDeliveryJpaEntity() : entity;
    target.setId(delivery.id().value());
    target.setTenantId(delivery.tenantId() == null ? null : delivery.tenantId().value());
    target.setNotificationId(delivery.notificationId().value());
    target.setChannel(delivery.channel());
    target.setRecipient(delivery.recipient());
    target.setStatus(delivery.status());
    target.setAttemptCount(delivery.attemptCount());
    target.setNextAttemptAt(delivery.nextAttemptAt());
    target.setLastAttemptAt(delivery.lastAttemptAt());
    target.setProvider(delivery.provider());
    target.setProviderMessageId(delivery.providerMessageId());
    target.setErrorCode(delivery.errorCode());
    target.setErrorMessage(delivery.errorMessage());
    target.setPayload(delivery.payload());
    target.setCreatedAt(delivery.createdAt());
    target.setUpdatedAt(delivery.updatedAt());
    return target;
  }

  static NotificationDelivery toDomain(NotificationDeliveryJpaEntity entity) {
    return new NotificationDelivery(
        NotificationDeliveryId.of(entity.getId()),
        TenantId.nullableOf(entity.getTenantId()),
        NotificationId.of(entity.getNotificationId()),
        entity.getChannel(),
        entity.getRecipient(),
        entity.getStatus(),
        entity.getAttemptCount(),
        entity.getNextAttemptAt(),
        entity.getLastAttemptAt(),
        entity.getProvider(),
        entity.getProviderMessageId(),
        entity.getErrorCode(),
        entity.getErrorMessage(),
        entity.getPayload(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  static NotificationDeliveryView toDeliveryView(NotificationDeliveryJpaEntity entity) {
    return new NotificationDeliveryView(
        NotificationDeliveryId.of(entity.getId()),
        NotificationId.of(entity.getNotificationId()),
        entity.getChannel(),
        entity.getRecipient(),
        entity.getStatus(),
        entity.getAttemptCount(),
        entity.getNextAttemptAt(),
        entity.getLastAttemptAt(),
        entity.getProvider(),
        entity.getProviderMessageId(),
        entity.getErrorCode(),
        entity.getErrorMessage(),
        entity.getCreatedAt());
  }
}
