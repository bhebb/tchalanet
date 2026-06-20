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
    name = "platform_user_role",
    indexes = {
      @Index(name = "ix_platform_user_role__user", columnList = "user_id"),
      @Index(name = "ix_platform_user_role__role", columnList = "role_id")
    })
@Getter
@Setter
public class PlatformUserRoleJpaEntity {

  @Id
  @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
  private UUID id;

  @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
  private UUID userId;

  @Column(name = "role_id", nullable = false, columnDefinition = "uuid")
  private UUID roleId;

  @Column(name = "assigned_at", nullable = false)
  private Instant assignedAt;

  @Column(name = "assigned_by", columnDefinition = "uuid")
  private UUID assignedBy;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @PrePersist
  void prePersist() {
    if (id == null) id = UUID.randomUUID();
    if (assignedAt == null) assignedAt = Instant.now();
  }
}
