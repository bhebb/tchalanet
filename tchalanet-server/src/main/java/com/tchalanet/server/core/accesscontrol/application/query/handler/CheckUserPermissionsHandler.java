package com.tchalanet.server.core.accesscontrol.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.core.accesscontrol.application.port.out.RolePermissionReaderPort;
import com.tchalanet.server.core.accesscontrol.application.port.out.TenantUserDirectoryPort;
import com.tchalanet.server.core.accesscontrol.application.query.model.CheckUserPermissionsQuery;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CheckUserPermissionsHandler
    implements QueryHandler<CheckUserPermissionsQuery, Boolean> {

  private final TenantUserDirectoryPort tenantUserDirectory;
  private final RolePermissionReaderPort rolePermissionReaderPort;

  @Override
  public Boolean handle(CheckUserPermissionsQuery query) {
    UUID tenantId = query.tenantId();
    UUID userId = query.userId();
    Set<String> required = query.requiredPermissions();

    // Cas trivial : si aucune permission demandée, on autorise
    if (tenantId == null || userId == null) {
      return false;
    }
    if (required == null || required.isEmpty()) {
      return true;
    }

    // Récupérer la membership active (rôle unique) pour ce user dans ce tenant
    var membershipOpt = tenantUserDirectory.findActiveMembership(tenantId, userId);
    if (membershipOpt.isEmpty()) {
      return false;
    }

    var membership = membershipOpt.get();
    UUID roleId = membership.roleId();

    // Résoudre les permissions accordées par la hiérarchie de ce rôle
    Set<String> granted = rolePermissionReaderPort.findPermissionCodesForRoleHierarchy(roleId);

    // Vérifier que toutes les permissions requises sont présentes
    return granted.containsAll(required);
  }
}
