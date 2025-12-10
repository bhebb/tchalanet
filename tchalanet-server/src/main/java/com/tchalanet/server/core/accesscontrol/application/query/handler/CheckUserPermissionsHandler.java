package com.tchalanet.server.core.accesscontrol.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.core.accesscontrol.application.port.out.PermissionCatalogPort;
import com.tchalanet.server.core.accesscontrol.application.port.out.TenantUserDirectoryPort;
import com.tchalanet.server.core.accesscontrol.application.query.model.CheckUserPermissionsQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CheckUserPermissionsHandler implements QueryHandler<CheckUserPermissionsQuery, Boolean> {

    private final TenantUserDirectoryPort tenantUserDirectory;
    private final PermissionCatalogPort permissionCatalog;

    @Override
    public Boolean handle(CheckUserPermissionsQuery query) {
        // Check if user is active in tenant
        if (!tenantUserDirectory.isUserActiveInTenant(query.userId(), query.tenantId())) {
            return false;
        }

        // Get user roles
        var roles = tenantUserDirectory.getUserRolesInTenant(query.userId(), query.tenantId());

        // Check if any role has the required permission
        return permissionCatalog.getPermissionsForRoles(roles)
            .contains(query.resource() + "." + query.action());
    }
}
