package com.tchalanet.server.core.accesscontrol.application.port.out;

import com.tchalanet.server.common.types.id.RoleId;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Port de sortie pour l'administration du catalogue de permissions et du mapping rôle -> permissions. */
public interface PermissionCatalogAdminPort {

  Set<String> getRolePermissions(RoleId roleId);

  List<PermissionSummary> listPermissions();

  record PermissionSummary(String code, String name, String category, String description) {}
}
