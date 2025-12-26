package com.tchalanet.server.core.accesscontrol.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.core.accesscontrol.application.port.out.RolePermissionReaderPort;
import com.tchalanet.server.core.accesscontrol.application.port.out.TenantUserDirectoryPort;
import com.tchalanet.server.core.accesscontrol.application.query.model.GetEffectivePermissionsQuery;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetEffectivePermissionsHandler
    implements QueryHandler<GetEffectivePermissionsQuery, List<String>> {

    private final TenantUserDirectoryPort tenantUserDirectory;
    private final RolePermissionReaderPort rolePermissionReaderPort;

    @Override
    public List<String> handle(GetEffectivePermissionsQuery query) {
        // Get user roles in tenant (ids de rôles)
        var roleIds = tenantUserDirectory.getUserRolesInTenant(query.userId(), query.tenantId());
        if (roleIds.isEmpty()) {
            return List.of();
        }
        // Agréger toutes les permissions à partir de la hiérarchie des rôles
        Set<String> all =
            roleIds.stream()
                .filter(Objects::nonNull)
                .flatMap(id -> rolePermissionReaderPort.findPermissionCodesForRoleHierarchy(id).stream())
                .collect(Collectors.toSet());
        return List.copyOf(all);
    }
}
