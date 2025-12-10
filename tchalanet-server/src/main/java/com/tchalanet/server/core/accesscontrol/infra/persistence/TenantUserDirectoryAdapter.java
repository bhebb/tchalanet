package com.tchalanet.server.core.accesscontrol.infra.persistence;

import com.tchalanet.server.core.accesscontrol.application.port.out.TenantUserDirectoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TenantUserDirectoryAdapter implements TenantUserDirectoryPort {

    private final TenantUserRepository tenantUserRepository;
    private final AppRoleRepository appRoleRepository;

    @Override
    public List<String> getUserRolesInTenant(String userId, String tenantId) {
        return tenantUserRepository.findByTenantIdAndUserId(UUID.fromString(tenantId), userId)
            .stream()
            .filter(entity -> entity.getDeletedAt() == null) // soft delete
            .map(entity -> {
                // Get role code from role_id
                return appRoleRepository.findById(entity.getRoleId())
                    .map(role -> role.getCode())
                    .orElse("UNKNOWN_ROLE");
            })
            .toList();
    }

    @Override
    public boolean isUserActiveInTenant(String userId, String tenantId) {
        return tenantUserRepository.findByTenantIdAndUserId(UUID.fromString(tenantId), userId)
            .stream()
            .anyMatch(entity -> entity.getDeletedAt() == null);
    }
}
