package com.tchalanet.server.core.accesscontrol.application.port.out;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Port de sortie pour l'administration du catalogue de permissions et du mapping rôle -> permissions. */
public interface PermissionCatalogAdminPort {

  Set<String> getRolePermissions(UUID roleId);

  List<PermissionSummary> listPermissions();

  record PermissionSummary(String code, String name, String category, String description) {}
}
