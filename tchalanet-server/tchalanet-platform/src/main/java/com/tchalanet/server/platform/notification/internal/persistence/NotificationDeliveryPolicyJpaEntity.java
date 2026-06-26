package com.tchalanet.server.platform.notification.internal.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import com.tchalanet.server.platform.notification.api.model.NotificationDeliveryChannel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notification_delivery_policy")
@Getter
@Setter
public class NotificationDeliveryPolicyJpaEntity extends BaseEntity {

  @Column(name = "notification_id", nullable = false, columnDefinition = "uuid")
  private UUID notificationId;

  @Column(name = "publication_id", columnDefinition = "uuid")
  private UUID publicationId;

  @Column(name = "tenant_id", columnDefinition = "uuid")
  private UUID tenantId;

  @Enumerated(EnumType.STRING)
  @Column(name = "channel", nullable = false, length = 32)
  private NotificationDeliveryChannel channel;

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;
}
