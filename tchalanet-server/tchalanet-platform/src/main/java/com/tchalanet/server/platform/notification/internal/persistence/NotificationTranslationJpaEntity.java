package com.tchalanet.server.platform.notification.internal.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notification_translation")
@Getter
@Setter
public class NotificationTranslationJpaEntity extends BaseEntity {

  @Column(name = "notification_id", nullable = false, columnDefinition = "uuid")
  private UUID notificationId;

  @Column(name = "tenant_id", columnDefinition = "uuid")
  private UUID tenantId;

  @Column(name = "locale", nullable = false, length = 10)
  private String locale;

  @Column(name = "title_text", nullable = false)
  private String titleText;

  @Column(name = "body_text")
  private String bodyText;
}
