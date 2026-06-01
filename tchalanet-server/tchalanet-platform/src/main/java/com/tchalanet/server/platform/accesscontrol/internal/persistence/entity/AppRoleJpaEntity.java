package com.tchalanet.server.platform.accesscontrol.internal.persistence.entity;

import com.tchalanet.server.common.persistence.BaseEntity;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

@Entity
@Table(
    name = "app_role",
    indexes = {@Index(name = "ix_app_role_tenant", columnList = "tenant_id")})
@Audited
@Getter
@Setter
public class AppRoleJpaEntity extends BaseEntity {

  @Column(name = "tenant_id", columnDefinition = "uuid")
  private UUID tenantId; // null = system role

  @Column(name = "code", nullable = false, length = 64)
  private String code;

  @Column(name = "name", nullable = false, length = 128)
  private String name;

  @Column(name = "description")
  private String description;

  @Column(name = "scope", nullable = false, length = 32)
  private String scope = "TENANT";

  @Column(name = "system", nullable = false)
  private boolean system = true;

  @Column(name = "custom", nullable = false)
  private boolean custom = false;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  @OneToMany(
      mappedBy = "role",
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  @NotAudited
  private List<AppRolePermissionJpaEntity> rolePermissions = new ArrayList<>();

  // Explicit accessors to help static analysis tools that may not resolve Lombok-generated methods
  public UUID getId() {
    return super.getId();
  }

  public void setId(UUID id) {
    super.setId(id);
  }
}

