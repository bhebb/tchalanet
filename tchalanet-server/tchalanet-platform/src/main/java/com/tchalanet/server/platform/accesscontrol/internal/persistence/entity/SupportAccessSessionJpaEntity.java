package com.tchalanet.server.platform.accesscontrol.internal.persistence.entity;

import com.tchalanet.server.common.persistence.BaseEntity;
import com.tchalanet.server.platform.accesscontrol.api.model.SupportAccessMode;
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

@Entity
@Table(
    name = "support_access_session",
    indexes = {
      @Index(name = "ix_support_access_session__super_admin", columnList = "super_admin_user_id"),
      @Index(name = "ix_support_access_session__tenant", columnList = "tenant_id")
    })
@Getter
@Setter
public class SupportAccessSessionJpaEntity extends BaseEntity {

  @Column(name = "super_admin_user_id", nullable = false, columnDefinition = "uuid")
  private UUID superAdminUserId;

  @Column(name = "tenant_id", nullable = false, columnDefinition = "uuid")
  private UUID tenantId;

  @Column(name = "tenant_code", nullable = false, length = 128)
  private String tenantCode;

  @Column(name = "tenant_name", nullable = false, length = 255)
  private String tenantName;

  @Enumerated(EnumType.STRING)
  @Column(name = "mode", nullable = false, length = 32)
  private SupportAccessMode mode;

  @Column(name = "reason", nullable = false, length = 1024)
  private String reason;

  @Column(name = "granted_at", nullable = false)
  private Instant grantedAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "cleared_at")
  private Instant clearedAt;
}
