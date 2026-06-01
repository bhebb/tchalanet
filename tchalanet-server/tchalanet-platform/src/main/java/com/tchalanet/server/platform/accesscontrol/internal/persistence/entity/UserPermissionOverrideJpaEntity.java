package com.tchalanet.server.platform.accesscontrol.internal.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "user_permission_override",
    indexes = {
      @Index(name = "ix_user_perm_override__tenant_user", columnList = "tenant_id, user_id"),
      @Index(name = "ix_user_perm_override__code", columnList = "permission_code")
    })
@Getter
@Setter
public class UserPermissionOverrideJpaEntity {

  @Id
  @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
  private UUID id;

  @Column(name = "tenant_id", nullable = false, columnDefinition = "uuid")
  private UUID tenantId;

  @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
  private UUID userId;

  @Column(name = "permission_code", nullable = false, length = 128)
  private String permissionCode;

  @Column(name = "effect", nullable = false, length = 16)
  private String effect; // GRANT or DENY

  @Column(name = "reason")
  private String reason;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "created_by", columnDefinition = "uuid")
  private UUID createdBy;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @PrePersist
  void prePersist() {
    if (id == null) id = UUID.randomUUID();
    if (createdAt == null) createdAt = Instant.now();
  }
}
