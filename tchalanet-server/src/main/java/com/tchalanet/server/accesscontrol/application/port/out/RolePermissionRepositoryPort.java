package com.tchalanet.server.accesscontrol.application.port.out;

import com.tchalanet.server.accesscontrol.domain.model.Permission;
import com.tchalanet.server.accesscontrol.domain.model.TchRole;
import java.util.Set;

public interface RolePermissionRepositoryPort {
  Set<Permission> getPermissionsForRoles(Set<TchRole> roles);
}
