package com.tchalanet.server.core.accesscontrol.application.query.handler;

import com.tchalanet.server.common.app.QueryHandler;
import com.tchalanet.server.core.accesscontrol.application.port.out.PermissionCatalogPort;
import com.tchalanet.server.core.accesscontrol.application.port.out.TenantUserDirectoryPort;
import com.tchalanet.server.core.accesscontrol.application.query.model.GetEffectivePermissionsQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GetEffectivePermissionsHandler implements QueryHandler<GetEffectivePermissionsQuery, List<String>> {

    private final TenantUserDirectoryPort tenantUserDirectory;
    private final PermissionCatalogPort permissionCatalog;

    @Override
    public List<String> handle(GetEffectivePermissionsQuery query) {
        // Get user roles in tenant
        var roles = tenantUserDirectory.getUserRolesInTenant(query.userId(), query.tenantId());

        // Get all permissions for those roles
        return permissionCatalog.getPermissionsForRoles(roles);
    }
}
