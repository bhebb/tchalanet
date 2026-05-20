package com.tchalanet.server.platform.accesscontrol.internal.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "role_permission")
@Getter
@Setter
public class AppRolePermissionJpaEntity {

  @EmbeddedId private AppRolePermissionId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("roleId")
  @JoinColumn(name = "role_id", nullable = false)
  private AppRoleJpaEntity role;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("permissionCode")
  @JoinColumn(name = "permission_code", referencedColumnName = "code", nullable = false)
  private PermissionJpaEntity permission;
}

