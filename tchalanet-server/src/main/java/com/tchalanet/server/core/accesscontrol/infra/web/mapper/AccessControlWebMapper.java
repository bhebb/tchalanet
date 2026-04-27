package com.tchalanet.server.core.accesscontrol.infra.web.mapper;

import com.tchalanet.server.common.types.enums.TchRole;
import com.tchalanet.server.core.accesscontrol.application.port.out.PermissionCatalogAdminPort.PermissionSummary;
import com.tchalanet.server.core.accesscontrol.infra.web.model.PermissionResponse;
import com.tchalanet.server.core.accesscontrol.infra.web.model.RoleAdminResponse;

public final class AccessControlWebMapper {

  private AccessControlWebMapper() {}

  public static RoleAdminResponse toRoleAdminResponse(TchRole role) {
    // Pour l'instant, on mappe simplement le nom de l'enum comme code/nom.
    String code = role.name();
    String name = role.name();
    return new RoleAdminResponse(null, code, name, null, null, null, true);
  }

  public static PermissionResponse toPermissionResponse(PermissionSummary p) {
    return new PermissionResponse(p.code(), p.name(), p.category(), p.description());
  }
}
