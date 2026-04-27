package com.tchalanet.server.features.notifications.shared;

public class NotificationMapper {

  private NotificationMapper() {}

  public static NotificationDto toDto(NotificationEntity entity) {
    return new NotificationDto(
        entity.getId(),
        entity.getUserId(),
        entity.getTenantId(),
        entity.getChannel(),
        entity.getType(),
        entity.getDisplayType(),
        entity.getTitle(),
        entity.getBody(),
        entity.isRead(),
        entity.getCreatedAt(),
        entity.getReadAt());
  }

  public static NotificationEntity toEntity(NotificationDto dto) {
    var entity = new NotificationEntity();
    entity.setId(dto.id());
    entity.setType(dto.type());
    entity.setDisplayType(dto.displayType());
    entity.setTitle(dto.title());
    entity.setBody(dto.body());
    entity.setRead(dto.read());
    entity.setCreatedAt(dto.createdAt());
    entity.setReadAt(dto.readAt());
    entity.setTenantId(dto.tenantId());
    entity.setUserId(dto.userId());
    entity.setChannel(dto.channel());
    return entity;
  }
}
