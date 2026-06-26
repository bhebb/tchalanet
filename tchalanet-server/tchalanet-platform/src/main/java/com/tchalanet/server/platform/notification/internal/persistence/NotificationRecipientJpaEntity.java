package com.tchalanet.server.platform.notification.internal.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import com.tchalanet.server.platform.notification.api.model.NotificationActorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notification_recipient")
@Getter
@Setter
public class NotificationRecipientJpaEntity extends BaseEntity {

  @Column(name = "notification_id", nullable = false, columnDefinition = "uuid")
  private UUID notificationId;

  @Column(name = "publication_id", nullable = false, columnDefinition = "uuid")
  private UUID publicationId;

  @Column(name = "tenant_id", columnDefinition = "uuid")
  private UUID tenantId;

  @Enumerated(EnumType.STRING)
  @Column(name = "recipient_actor_type", nullable = false, length = 32)
  private NotificationActorType recipientActorType;

  @Column(name = "recipient_actor_id", nullable = false, columnDefinition = "uuid")
  private UUID recipientActorId;

  @Column(name = "delivered_at")
  private Instant deliveredAt;

  @Column(name = "seen_at")
  private Instant seenAt;

  @Column(name = "read_at")
  private Instant readAt;

  @Column(name = "dismissed_at")
  private Instant dismissedAt;
}
