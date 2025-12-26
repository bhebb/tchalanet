package com.tchalanet.server.features.notifications.shared;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.common.types.enums.NotificationChannel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Entité JPA pour les notifications utilisateur (inbox UI). */
@Entity
@Table(name = "user_notification")
@Getter
@Setter
public class NotificationEntity extends BaseTenantEntity {

  private String title;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 64)
  private NotificationType type; // import depuis core.notification

  /** Catégorie fonctionnelle, ex: "SYSTEM", "SALES", "LIMIT". */
  private String category;

  @Enumerated(EnumType.STRING)
  @Column(name = "display_type", nullable = false, length = 32)
  private NotificationDisplayType displayType;

  @Enumerated(EnumType.STRING)
  private NotificationChannel channel;

  @Column(name = "body", nullable = false, length = 4000)
  private String body;

  @Column(name = "payload_json", columnDefinition = "jsonb")
  private String payloadJson; // données supplémentaires (clé-valeur rendu front)

  @Column(name = "is_read", nullable = false)
  private boolean read;

  @Column(name = "read_at")
  private Instant readAt;
}
