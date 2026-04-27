package com.tchalanet.server.core.accesscontrol.application.port.out;

import com.tchalanet.server.common.types.enums.TchRole;
import com.tchalanet.server.core.accesscontrol.domain.model.Permission;
import java.util.Set;

public interface RolePermissionRepositoryPort {
  Set<Permission> getPermissionsForRoles(Set<TchRole> roles);
}
