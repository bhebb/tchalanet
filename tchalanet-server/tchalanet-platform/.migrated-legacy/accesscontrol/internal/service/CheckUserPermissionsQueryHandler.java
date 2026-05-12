package com.tchalanet.server.platform.accesscontrol.internal.service;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CheckUserPermissionsRequestHandler
    implements QueryHandler<CheckUserPermissionsRequest, CheckUserPermissionsResult> {

  private final TenantUserDirectoryPort tenantUserDirectory;
  private final RolePermissionReaderPort rolePermissionReaderPort;

  @Override
  public CheckUserPermissionsResult handle(CheckUserPermissionsRequest query) {
    TenantId tenantId = query.tenantId();
    UserId userId = query.userId();
    Set<String> required = query.requiredPermissions();

    // Cas trivial : si aucune permission demandée, on autorise
    if (tenantId == null || userId == null) {
      return new CheckUserPermissionsResult(false, required == null ? Set.of() : required);
    }
    if (required == null || required.isEmpty()) {
      return new CheckUserPermissionsResult(true, Set.of());
    }

    // Récupérer la membership active (rôle unique) pour ce user dans ce tenant
    var membershipOpt = tenantUserDirectory.findActiveMembership(tenantId, userId);
    if (membershipOpt.isEmpty()) {
      return new CheckUserPermissionsResult(false, required);
    }

    var membership = membershipOpt.get();
    var roleId = membership.roleId();

    // Résoudre les permissions accordées par la hiérarchie de ce rôle
    Set<String> granted = rolePermissionReaderPort.findPermissionCodesForRoleHierarchy(roleId);

    // Vérifier que toutes les permissions requises sont présentes
    Set<String> missing =
        required.stream().filter(permission -> !granted.contains(permission)).collect(Collectors.toSet());

    return new CheckUserPermissionsResult(missing.isEmpty(), missing);
  }
}

