package com.tchalanet.server.platform.accesscontrol.internal.service;

import com.tchalanet.server.common.bus.QueryHandler;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetEffectivePermissionsRequestHandler
    implements QueryHandler<GetEffectivePermissionsRequest, EffectivePermissionsView> {

  private final TenantUserDirectoryPort tenantUserDirectory;
  private final RolePermissionReaderPort rolePermissionReaderPort;

  @Override
  public EffectivePermissionsView handle(GetEffectivePermissionsRequest query) {
    // Get user roles in tenant (ids de rôles)
    var roleIds = tenantUserDirectory.getUserRolesInTenant(query.userId(), query.tenantId());
    if (roleIds.isEmpty()) {
      return new EffectivePermissionsView(query.tenantId(), query.userId(), null, Set.of());
    }
    // Agréger toutes les permissions à partir de la hiérarchie des rôles
    Set<String> all =
        roleIds.stream()
            .filter(Objects::nonNull)
            .flatMap(
                id -> rolePermissionReaderPort.findPermissionCodesForRoleHierarchy(id).stream())
            .collect(Collectors.toSet());
    return new EffectivePermissionsView(query.tenantId(), query.userId(), roleIds.getFirst(), all);
  }
}

