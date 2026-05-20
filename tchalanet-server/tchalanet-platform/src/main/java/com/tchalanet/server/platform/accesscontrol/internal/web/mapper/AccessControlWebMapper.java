package com.tchalanet.server.platform.accesscontrol.internal.web.mapper;

import com.tchalanet.server.platform.accesscontrol.api.model.view.PermissionView;
import com.tchalanet.server.platform.accesscontrol.api.model.view.RoleView;
import com.tchalanet.server.platform.accesscontrol.internal.web.model.PermissionResponse;
import com.tchalanet.server.platform.accesscontrol.internal.web.model.RoleAdminResponse;

public final class AccessControlWebMapper {

  private AccessControlWebMapper() {}

  public static RoleAdminResponse toRoleAdminResponse(RoleView role) {
    return new RoleAdminResponse(
        role.id() == null ? null : role.id().value(),
        role.code(),
        role.name(),
        role.description(),
        role.tenantId() == null ? null : role.tenantId().value(),
        role.parentRoleId() == null ? null : role.parentRoleId().value(),
        role.system());
  }

  public static PermissionResponse toPermissionResponse(PermissionView permission) {
    return new PermissionResponse(
        permission.code(),
        permission.name(),
        permission.category(),
        permission.description());
  }
}


