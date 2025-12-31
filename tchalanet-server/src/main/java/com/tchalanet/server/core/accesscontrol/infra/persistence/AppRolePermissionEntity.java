package com.tchalanet.server.core.accesscontrol.infra.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "role_permission")
@Getter
@Setter
public class AppRolePermissionEntity {

  @EmbeddedId private AppRolePermissionId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("roleId")
  @JoinColumn(name = "role_id", nullable = false)
  private AppRoleEntity role;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("permissionCode")
  @JoinColumn(name = "permission_code", referencedColumnName = "code", nullable = false)
  private PermissionEntity permission;
}
