package com.tchalanet.server.core.tenant.infra.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(
    name = "tenant_user",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "user_id"}))
@Audited
@Getter
@Setter
@NoArgsConstructor
public class TenantUserJpaEntity extends BaseEntity {

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "role", nullable = false, length = 32)
  private String role;

  @Column(name = "autonomy_level", nullable = false, length = 16)
  private String autonomyLevel = "none";

  @Column(name = "is_owner", nullable = false)
  private boolean owner = false;

  @Column(name = "version", nullable = false)
  private long version = 0L;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "created_by")
  private UUID createdBy;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "updated_by")
  private UUID updatedBy;

  @Column(name = "deleted_at")
  private Instant deletedAt;
}
