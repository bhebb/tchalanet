package com.tchalanet.server.platform.accesscontrol.internal.persistence.entity;

import com.tchalanet.server.common.persistence.AuditableEntity;
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
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

@Entity
@Table(name = "permission")
@Getter
@Setter
@Audited
public class PermissionJpaEntity extends AuditableEntity {

  @Id
  @Column(name = "code", nullable = false, length = 128)
  private String code;

  @Column(name = "name", nullable = false, length = 128)
  private String name;

  @Column(name = "category", length = 64)
  private String category;

  @Column(name = "description")
  private String description;

  @Column(name = "system", nullable = false)
  private boolean system = true;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  // Inverse side for role_permission -> permission
  @OneToMany(
      mappedBy = "permission",
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  @NotAudited
  private List<AppRolePermissionJpaEntity> rolePermissions = new ArrayList<>();
}

