package com.tchalanet.server.platform.notification.internal.mapper;

import com.tchalanet.server.common.types.id.NotificationId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.notification.api.model.NotificationAudienceType;
import com.tchalanet.server.platform.notification.api.model.view.NotificationActionView;
import com.tchalanet.server.platform.notification.api.model.view.NotificationItemView;
import com.tchalanet.server.platform.notification.internal.persistence.NotificationJpaEntity;
import com.tchalanet.server.platform.notification.internal.service.Notification;
import com.tchalanet.server.platform.notification.internal.service.NotificationAction;
import java.util.Set;

public final class NotificationMapper {

  private NotificationMapper() {}

  public static NotificationJpaEntity toEntity(Notification notification, NotificationJpaEntity entity) {
    var target = entity == null ? new NotificationJpaEntity() : entity;
    target.setId(notification.id().value());
    target.setTenantId(notification.tenantId() == null ? null : notification.tenantId().value());
    target.setScope(scope(notification));
    target.setType(type(notification));
    target.setSourceType(sourceType(notification.sourceType()));
    target.setSourceId(notification.sourceId());
    target.setDedupeKey(notification.dedupeKey());
    target.setAudienceType(notification.audienceType());
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
    target.setActionRoute(notification.action() == null ? null : notification.action().url());
    target.setStatus(notification.status());
    target.setExpiresAt(notification.expiresAt());
    target.setCreatedAt(notification.createdAt());
    target.setUpdatedAt(notification.updatedAt());
	    return target;
	  }

  private static String scope(Notification notification) {
    if (notification.tenantId() != null) {
      return "TENANT";
    }
    if (notification.audienceType() != null
        && notification.audienceType() == NotificationAudienceType.PLATFORM_ADMINS) {
      return "PLATFORM";
    }
    return "GLOBAL";
  }

  private static String type(Notification notification) {
    if (notification.sourceId() != null && !notification.sourceId().isBlank()) {
      return truncate(notification.sourceId(), 64);
    }
    if (notification.kind() != null) {
      return truncate(notification.kind().name(), 64);
    }
    return "SYSTEM_ANNOUNCEMENT";
  }

  private static String sourceType(String sourceType) {
    if (sourceType == null || sourceType.isBlank()) {
      return "SYSTEM";
    }
    return switch (sourceType) {
      case "SYSTEM", "ADMIN", "SUPER_ADMIN", "OPS", "SUPPORT", "DOMAIN_EVENT" -> sourceType;
      default -> "ADMIN";
    };
  }

  private static String truncate(String value, int maxLength) {
    return value.length() <= maxLength ? value : value.substring(0, maxLength);
  }

  public static Notification toDomain(NotificationJpaEntity entity) {
    return new Notification(
        NotificationId.of(entity.getId()),
        TenantId.nullableOf(entity.getTenantId()),
        entity.getSourceType(),
        entity.getSourceId(),
        entity.getDedupeKey(),
        entity.getAudienceType(),
        Set.of(),
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
        entity.getExpiresAt(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  public static NotificationItemView toItemView(NotificationJpaEntity entity) {
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
        null,
        null,
        entity.getExpiresAt(),
        entity.getCreatedAt());
  }
}
