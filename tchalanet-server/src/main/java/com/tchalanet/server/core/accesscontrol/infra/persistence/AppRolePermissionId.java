package com.tchalanet.server.core.accesscontrol.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class AppRolePermissionId implements Serializable {

  @Column(name = "role_id", nullable = false)
  private UUID roleId;

  @Column(name = "permission_code", nullable = false, length = 128)
  private String permissionCode;

  protected AppRolePermissionId() {}

  public AppRolePermissionId(UUID roleId, String permissionCode) {
    this.roleId = roleId;
    this.permissionCode = permissionCode;
  }

  public UUID getRoleId() {
    return roleId;
  }

  public String getPermissionCode() {
    return permissionCode;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AppRolePermissionId that = (AppRolePermissionId) o;
    return Objects.equals(roleId, that.roleId)
        && Objects.equals(permissionCode, that.permissionCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roleId, permissionCode);
  }
}
