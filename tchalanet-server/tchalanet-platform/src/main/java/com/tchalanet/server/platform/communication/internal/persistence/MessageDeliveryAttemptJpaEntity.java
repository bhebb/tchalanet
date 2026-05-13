package com.tchalanet.server.platform.communication.internal.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import com.tchalanet.server.platform.communication.api.model.value.DeliveryStatus;
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
@Table(name = "message_delivery_attempt")
@Getter
@Setter
public class MessageDeliveryAttemptJpaEntity extends BaseEntity {

  @Column(name = "message_id", nullable = false, columnDefinition = "uuid")
  private UUID messageId;

  @Column(name = "attempted_at", nullable = false)
  private Instant attemptedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private DeliveryStatus status;

  @Column(name = "provider", nullable = false, length = 80)
  private String provider;

  @Column(name = "provider_message_id", length = 255)
  private String providerMessageId;

  @Column(name = "error_code", length = 120)
  private String errorCode;

  @Column(name = "error_message", columnDefinition = "text")
  private String errorMessage;
}
