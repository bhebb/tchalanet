package com.tchalanet.server.core.accesscontrol.application;

import com.tchalanet.server.core.accesscontrol.application.port.in.GetEffectivePermissionsUseCase;
import com.tchalanet.server.core.accesscontrol.application.port.out.PermissionCatalogPort;
import com.tchalanet.server.core.accesscontrol.application.port.out.TenantUserDirectoryPort;
import com.tchalanet.server.core.accesscontrol.domain.model.EffectivePermissions;
import com.tchalanet.server.core.accesscontrol.domain.model.Permission;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetEffectivePermissionsService implements GetEffectivePermissionsUseCase {

  private final TenantUserDirectoryPort tenantUserDirectoryPort;
  private final PermissionCatalogPort permissionCatalogPort;

  @Override
  public EffectivePermissions getEffectivePermissions(UUID tenantId, UUID userId) {
    var membershipOpt = tenantUserDirectoryPort.findByTenantAndUser(tenantId, userId);

    if (membershipOpt.isEmpty()) {
      // Pas de membership = pas de droits dans ce tenant.
      // On retourne un EffectivePermissions "vide".
      return new EffectivePermissions(tenantId, userId, null, Set.of());
    }

    var membership = membershipOpt.get();

    var permissions =
        permissionCatalogPort.findPermissionsForRoleHierarchy(membership.roleId()).stream()
            .map(Permission::new)
            .collect(Collectors.toSet());

    return new EffectivePermissions(
        membership.tenantId(), membership.userId(), membership.roleId(), permissions);
  }
}
