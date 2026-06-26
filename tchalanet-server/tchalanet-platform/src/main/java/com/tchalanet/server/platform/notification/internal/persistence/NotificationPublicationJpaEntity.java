package com.tchalanet.server.platform.notification.internal.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import com.tchalanet.server.platform.notification.api.model.NotificationAudienceType;
import com.tchalanet.server.platform.notification.api.model.NotificationPublicationStatus;
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
@Table(name = "notification_publication")
@Getter
@Setter
public class NotificationPublicationJpaEntity extends BaseEntity {

  @Column(name = "notification_id", nullable = false, columnDefinition = "uuid")
  private UUID notificationId;

  @Column(name = "tenant_id", columnDefinition = "uuid")
  private UUID tenantId;

  @Column(name = "publication_no", nullable = false)
  private int publicationNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private NotificationPublicationStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "audience_type", nullable = false, length = 64)
  private NotificationAudienceType audienceType;

  @Column(name = "published_at", nullable = false)
  private Instant publishedAt;

  @Column(name = "expires_at")
  private Instant expiresAt;

  @Column(name = "republished_from_publication_id", columnDefinition = "uuid")
  private UUID republishedFromPublicationId;

  @Column(name = "reason")
  private String reason;

  @Enumerated(EnumType.STRING)
  @Column(name = "created_by_actor_type", length = 32)
  private NotificationActorType createdByActorType;

  @Column(name = "created_by_actor_id", columnDefinition = "uuid")
  private UUID createdByActorId;
}
