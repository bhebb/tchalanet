package com.tchalanet.server.platform.notification.internal.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import com.tchalanet.server.core.notification.domain.model.NotificationAudienceType;
import com.tchalanet.server.core.notification.domain.model.NotificationCategory;
import com.tchalanet.server.core.notification.domain.model.NotificationKind;
import com.tchalanet.server.core.notification.domain.model.NotificationSeverity;
import com.tchalanet.server.core.notification.domain.model.NotificationStatus;
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
    name = "notification",
    indexes = {
      @Index(
          name = "idx_notification_audience_status_created",
          columnList = "tenant_id,audience_type,audience_value,status,created_at"),
      @Index(name = "idx_notification_dedupe", columnList = "tenant_id,dedupe_key")
    })
@Getter
@Setter
@Audited
public class NotificationJpaEntity extends BaseEntity {

  @Column(name = "tenant_id", columnDefinition = "uuid")
  private UUID tenantId;

  @Column(name = "source_type", length = 96)
  private String sourceType;

  @Column(name = "source_id", length = 160)
  private String sourceId;

  @Column(name = "dedupe_key", length = 240)
  private String dedupeKey;

  @Enumerated(EnumType.STRING)
  @Column(name = "audience_type", nullable = false, length = 32)
  private NotificationAudienceType audienceType;

  @Column(name = "audience_value", nullable = false, length = 160)
  private String audienceValue;

  @Enumerated(EnumType.STRING)
  @Column(name = "severity", nullable = false, length = 32)
  private NotificationSeverity severity;

  @Enumerated(EnumType.STRING)
  @Column(name = "kind", nullable = false, length = 32)
  private NotificationKind kind;

  @Enumerated(EnumType.STRING)
  @Column(name = "category", nullable = false, length = 48)
  private NotificationCategory category;

  @Column(name = "title_key", length = 255)
  private String titleKey;

  @Column(name = "message_key", length = 255)
  private String messageKey;

  @Column(name = "title_text", length = 512)
  private String titleText;

  @Column(name = "message_text", length = 4000)
  private String messageText;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "payload", columnDefinition = "jsonb")
  private JsonNode payload;

  @Column(name = "action_type", length = 96)
  private String actionType;

  @Column(name = "action_url", length = 1024)
  private String actionUrl;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private NotificationStatus status;

  @Column(name = "read_at")
  private Instant readAt;

  @Column(name = "archived_at")
  private Instant archivedAt;

  @Column(name = "expires_at")
  private Instant expiresAt;
}
