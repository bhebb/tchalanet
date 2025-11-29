package com.tchalanet.server.core.accesscontrol.infra.web.mapper;

import com.tchalanet.server.core.accesscontrol.application.port.in.PermissionAdminUseCase;
import com.tchalanet.server.core.accesscontrol.application.port.in.RoleAdminUseCase;
import com.tchalanet.server.core.accesscontrol.infra.web.dto.PermissionResponse;
import com.tchalanet.server.core.accesscontrol.infra.web.dto.RoleAdminResponse;

public final class AccessControlWebMapper {

  private AccessControlWebMapper() {}

  public static RoleAdminResponse toRoleAdminResponse(RoleAdminUseCase.RoleSummary r) {
    return new RoleAdminResponse(
        r.id(), r.code(), r.name(), r.description(), r.tenantId(), r.parentRoleId(), r.system());
  }

  public static PermissionResponse toPermissionResponse(
      PermissionAdminUseCase.PermissionSummary p) {
    return new PermissionResponse(p.code(), p.name(), p.category(), p.description());
  }
}
