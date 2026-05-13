package com.tchalanet.server.platform.communication.internal.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tenant_communication_settings")
@Getter
@Setter
public class CommunicationSettingsJpaEntity extends BaseEntity {

  @Column(name = "tenant_id", nullable = false, unique = true, columnDefinition = "uuid")
  private UUID tenantId;

  @Column(name = "email_enabled", nullable = false)
  private boolean emailEnabled = true;

  @Column(name = "sms_enabled", nullable = false)
  private boolean smsEnabled;

  @Column(name = "tenant_slack_enabled", nullable = false)
  private boolean tenantSlackEnabled;

  @Column(name = "tenant_slack_webhook_secret_ref", length = 255)
  private String tenantSlackWebhookSecretRef;

  @Column(name = "critical_alert_email", length = 255)
  private String criticalAlertEmail;

  @Column(name = "ops_alert_email", length = 255)
  private String opsAlertEmail;

  @Column(name = "default_locale", nullable = false, length = 20)
  private String defaultLocale = "fr";

  @Column(name = "quiet_hours_start")
  private LocalTime quietHoursStart;

  @Column(name = "quiet_hours_end")
  private LocalTime quietHoursEnd;
}
