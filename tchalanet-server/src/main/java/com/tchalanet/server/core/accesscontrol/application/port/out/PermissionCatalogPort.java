package com.tchalanet.server.core.accesscontrol.application.port.out;

import java.util.Set;
import java.util.UUID;

public interface PermissionCatalogPort {
  Set<String> findPermissionsForRoleHierarchy(UUID roleId);
}
