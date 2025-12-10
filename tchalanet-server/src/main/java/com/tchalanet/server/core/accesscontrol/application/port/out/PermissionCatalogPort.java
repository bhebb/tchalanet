package com.tchalanet.server.core.accesscontrol.application.port.out;

import java.util.List;

public interface PermissionCatalogPort {
    List<String> getPermissionsForRoles(List<String> roleCodes);
    boolean hasPermission(String roleCode, String permissionCode);
}
