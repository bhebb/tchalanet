package com.tchalanet.server.core.accesscontrol.infra.persistence;

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
public class AppRoleEntity extends BaseEntity {

  @Column(name = "tenant_id", columnDefinition = "uuid")
  private UUID tenantId; // null = system role

  @Column(name = "code", nullable = false, length = 64)
  private String code;

  @Column(name = "name", nullable = false, length = 128)
  private String name;

  @Column(name = "description")
  private String description;

  @Column(name = "parent_role_id", columnDefinition = "uuid")
  private UUID parentRoleId;

  @OneToMany(
      mappedBy = "role",
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  @NotAudited
  private List<AppRolePermissionEntity> rolePermissions = new ArrayList<>();

  // Explicit accessors to help static analysis tools that may not resolve Lombok-generated methods
  public UUID getId() {
    return super.getId();
  }

  public void setId(UUID id) {
    super.setId(id);
  }
}
