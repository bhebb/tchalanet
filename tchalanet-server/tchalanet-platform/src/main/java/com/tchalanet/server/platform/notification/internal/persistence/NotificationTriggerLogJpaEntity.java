package com.tchalanet.server.platform.notification.internal.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notification_trigger_log")
@Getter
@Setter
public class NotificationTriggerLogJpaEntity extends BaseEntity {

  @Column(name = "tenant_id", columnDefinition = "uuid")
  private UUID tenantId;

  @Column(name = "trigger_key", nullable = false, length = 128)
  private String triggerKey;

  @Column(name = "source_type", nullable = false, length = 64)
  private String sourceType;

  @Column(name = "source_id", nullable = false, length = 128)
  private String sourceId;

  @Column(name = "notification_id", columnDefinition = "uuid")
  private UUID notificationId;

  @Column(name = "publication_id", columnDefinition = "uuid")
  private UUID publicationId;
}
