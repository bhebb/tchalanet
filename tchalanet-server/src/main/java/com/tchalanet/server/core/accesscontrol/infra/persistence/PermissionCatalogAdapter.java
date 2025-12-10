package com.tchalanet.server.core.accesscontrol.infra.persistence;

import com.tchalanet.server.common.persistence.PermissionRepository;
import com.tchalanet.server.core.accesscontrol.application.port.out.PermissionCatalogPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PermissionCatalogAdapter implements PermissionCatalogPort {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final AppRoleRepository appRoleRepository;

    @Override
    public List<String> getPermissionsForRoles(List<String> roleCodes) {
        // Get role IDs from codes
        List<UUID> roleIds = appRoleRepository.findAllByCodeIn(roleCodes)
            .stream()
            .map(role -> role.getId())
            .collect(Collectors.toList());

        // Get permission codes from role_permissions
        return rolePermissionRepository.findByRoleIds(roleIds)
            .stream()
            .map(rp -> rp.getPermissionCode())
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public boolean hasPermission(String roleCode, String permissionCode) {
        // Get role ID
        UUID roleId = appRoleRepository.findByCode(roleCode)
            .map(role -> role.getId())
            .orElse(null);

        if (roleId == null) return false;

        // Check if role has permission
        return rolePermissionRepository.findByRoleId(roleId)
            .stream()
            .anyMatch(rp -> rp.getPermissionCode().equals(permissionCode));
    }
}
