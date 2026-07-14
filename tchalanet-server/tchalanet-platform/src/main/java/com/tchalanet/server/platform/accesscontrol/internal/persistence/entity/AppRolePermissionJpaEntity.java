package com.tchalanet.server.platform.accesscontrol.internal.persistence.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
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
