package com.tchalanet.server.platform.notification.internal.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import com.tchalanet.server.core.notification.domain.model.NotificationChannel;
import com.tchalanet.server.core.notification.domain.model.NotificationDeliveryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

@Entity
@Table(
    name = "notification_delivery",
    indexes = {
      @Index(name = "idx_notification_delivery_notification_channel", columnList = "notification_id,channel"),
      @Index(name = "idx_notification_delivery_status_next", columnList = "status,next_attempt_at")
    })
@Getter
@Setter
@Audited
public class NotificationDeliveryJpaEntity extends BaseEntity {

  @Column(name = "tenant_id", columnDefinition = "uuid")
  private UUID tenantId;

  @Column(name = "notification_id", nullable = false, columnDefinition = "uuid")
  private UUID notificationId;

  @Enumerated(EnumType.STRING)
  @Column(name = "channel", nullable = false, length = 32)
  private NotificationChannel channel;

  @Column(name = "recipient", length = 255)
  private String recipient;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private NotificationDeliveryStatus status;

  @Column(name = "attempt_count", nullable = false)
  private int attemptCount;

  @Column(name = "next_attempt_at")
  private Instant nextAttemptAt;

  @Column(name = "last_attempt_at")
  private Instant lastAttemptAt;

  @Column(name = "provider", length = 96)
  private String provider;

  @Column(name = "provider_message_id", length = 160)
  private String providerMessageId;

  @Column(name = "error_code", length = 96)
  private String errorCode;

  @Column(name = "error_message", length = 1000)
  private String errorMessage;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "payload", columnDefinition = "jsonb")
  private JsonNode payload;
}
