package com.tchalanet.server.platform.notification.internal.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(
    name = "notification_template",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_notification_template__scope",
        columnNames = {"tenant_id", "template_key", "locale"}))
@Getter
@Setter
public class NotificationTemplateJpaEntity extends BaseEntity {

  @Column(name = "tenant_id", columnDefinition = "uuid")
  private UUID tenantId;

  @Column(name = "template_key", nullable = false, length = 120)
  private String templateKey;

  @Column(name = "locale", nullable = false, length = 20)
  private String locale;

  @Column(name = "title_template", nullable = false, columnDefinition = "text")
  private String titleTemplate;

  @Column(name = "body_template", nullable = false, columnDefinition = "text")
  private String bodyTemplate;

  @Column(name = "active", nullable = false)
  private boolean active = true;
}
