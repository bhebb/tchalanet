package com.tchalanet.server.platform.notification.internal.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import com.tchalanet.server.platform.notification.api.model.NotificationAudienceType;
import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import com.tchalanet.server.platform.notification.api.model.NotificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "notification")
@Getter
@Setter
public class NotificationJpaEntity extends BaseEntity {

  @Column(name = "tenant_id", columnDefinition = "uuid")
  private UUID tenantId;

  @Column(name = "scope", nullable = false, length = 32)
  private String scope;

  @Column(name = "type", nullable = false, length = 64)
  private String type;

  @Column(name = "source_type", length = 32)
  private String sourceType;

  @Column(name = "source_id", length = 128)
  private String sourceId;

  @Column(name = "dedupe_key", length = 240)
  private String dedupeKey;

  @Enumerated(EnumType.STRING)
  @Column(name = "audience_type", nullable = false, length = 64)
  private NotificationAudienceType audienceType;

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

  @Column(name = "action_route", length = 240)
  private String actionRoute;

  @Column(name = "action_label_key", length = 160)
  private String actionLabelKey;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private NotificationStatus status;

  @Column(name = "expires_at")
  private Instant expiresAt;
}
