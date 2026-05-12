package com.tchalanet.server.platform.notification.internal.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationChannel;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.internal.service.NotificationPreferenceScopeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "notification_preference")
@Getter
@Setter
@Audited
public class NotificationPreferenceJpaEntity extends BaseEntity {

  @Column(name = "tenant_id", nullable = false, columnDefinition = "uuid")
  private UUID tenantId;

  @Enumerated(EnumType.STRING)
  @Column(name = "scope_type", nullable = false, length = 32)
  private NotificationPreferenceScopeType scopeType;

  @Column(name = "scope_value", nullable = false, length = 160)
  private String scopeValue;

  @Enumerated(EnumType.STRING)
  @Column(name = "category", nullable = false, length = 48)
  private NotificationCategory category;

  @Enumerated(EnumType.STRING)
  @Column(name = "kind", nullable = false, length = 32)
  private NotificationKind kind;

  @Enumerated(EnumType.STRING)
  @Column(name = "channel", nullable = false, length = 32)
  private NotificationChannel channel;

  @Column(name = "enabled", nullable = false)
  private boolean enabled;
}
