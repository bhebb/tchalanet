package com.tchalanet.server.platform.communication.internal.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import com.tchalanet.server.platform.communication.api.model.value.DeliveryStatus;
import com.tchalanet.server.platform.communication.api.model.value.MessagePriority;
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
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

@Entity
@Table(
    name = "outbound_message",
    indexes = {
      @Index(name = "idx_outbound_message_pending", columnList = "status,next_attempt_at,priority")
    })
@Getter
@Setter
public class OutboundMessageJpaEntity extends BaseEntity {

  @Column(name = "tenant_id", columnDefinition = "uuid")
  private UUID tenantId;

  @Column(name = "source_event_id", columnDefinition = "uuid")
  private UUID sourceEventId;

  @Enumerated(EnumType.STRING)
  @Column(name = "channel", nullable = false, length = 32)
  private CommunicationChannel channel;

  @Column(name = "recipient_type", nullable = false, length = 32)
  private String recipientType;

  @Column(name = "recipient_value", nullable = false, length = 255)
  private String recipientValue;

  @Column(name = "template_key", nullable = false, length = 120)
  private String templateKey;

  @Column(name = "locale", length = 20)
  private String locale;

  @Column(name = "subject", length = 255)
  private String subject;

  @Column(name = "body", nullable = false, columnDefinition = "text")
  private String body;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
  private JsonNode payload;

  @Enumerated(EnumType.STRING)
  @Column(name = "priority", nullable = false, length = 32)
  private MessagePriority priority;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private DeliveryStatus status;

  @Column(name = "correlation_key", length = 180)
  private String correlationKey;

  @Column(name = "next_attempt_at")
  private Instant nextAttemptAt;

  @Column(name = "sent_at")
  private Instant sentAt;

  @Column(name = "failed_at")
  private Instant failedAt;

  @Column(name = "failure_reason", columnDefinition = "text")
  private String failureReason;
}
