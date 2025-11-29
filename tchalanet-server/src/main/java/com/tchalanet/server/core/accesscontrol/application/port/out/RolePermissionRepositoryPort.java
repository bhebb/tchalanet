package com.tchalanet.server.core.accesscontrol.application.port.out;

import com.tchalanet.server.core.accesscontrol.domain.model.Permission;
import com.tchalanet.server.core.accesscontrol.domain.model.TchRole;
import java.util.Set;

public interface RolePermissionRepositoryPort {
  Set<Permission> getPermissionsForRoles(Set<TchRole> roles);
}
