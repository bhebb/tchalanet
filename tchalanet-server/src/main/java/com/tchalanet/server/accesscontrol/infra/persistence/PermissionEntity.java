package com.tchalanet.server.accesscontrol.infra.persistence;

import com.tchalanet.server.common.infra.persistence.AuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "permission")
@Getter
@Setter
public class PermissionEntity extends AuditableEntity {

  @Id
  @Column(name = "code", nullable = false, length = 128)
  private String code;

  @Column(name = "name", nullable = false, length = 128)
  private String name;

  @Column(name = "category", length = 64)
  private String category;

  @Column(name = "description")
  private String description;

  // Inverse side for role_permission -> permission
  @OneToMany(
      mappedBy = "permission",
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  private List<AppRolePermissionEntity> rolePermissions = new ArrayList<>();
}
