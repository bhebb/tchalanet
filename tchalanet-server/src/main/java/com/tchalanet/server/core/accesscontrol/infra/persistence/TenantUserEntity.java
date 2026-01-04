package com.tchalanet.server.core.accesscontrol.infra.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(
    name = "tenant_user",
    uniqueConstraints =
        @UniqueConstraint(
            name = "ux_tenant_user_tenant_user",
            columnNames = {"tenant_id", "user_id"}),
    indexes = {
      @Index(name = "ix_tenant_user_tenant", columnList = "tenant_id"),
      @Index(name = "ix_tenant_user_user", columnList = "user_id")
    })
@Audited
@Getter
@Setter
public class TenantUserEntity extends BaseEntity {

  @Column(name = "tenant_id", nullable = false, columnDefinition = "uuid")
  private UUID tenantId;

  @Column(name = "user_id", nullable = false, length = 128)
  private String userId;

  @Column(name = "role_id", nullable = false, columnDefinition = "uuid")
  private UUID roleId;

  @Column(name = "status", nullable = false, length = 32)
  private String status; // INVITED|PENDING_APPROVAL|ACTIVE|SUSPENDED

  @Column(name = "autonomy_level", nullable = false, length = 16)
  private String autonomyLevel; // none|partial|full

  @Column(name = "is_owner", nullable = false)
  private Boolean owner = false;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "approved_by", columnDefinition = "uuid")
  private UUID approvedBy;
}
