package com.tchalanet.server.accesscontrol.application;

import com.tchalanet.server.accesscontrol.application.port.in.CheckUserPermissionsUseCase;
import com.tchalanet.server.accesscontrol.application.port.out.PermissionCatalogPort;
import com.tchalanet.server.accesscontrol.application.port.out.TenantUserDirectoryPort;
import com.tchalanet.server.accesscontrol.domain.exception.PermissionsDeniedException;
import com.tchalanet.server.accesscontrol.domain.model.CheckPermissionsResult;
import com.tchalanet.server.accesscontrol.domain.model.EffectivePermissions;
import com.tchalanet.server.accesscontrol.domain.model.Permission;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.featureflags.domain.model.FeatureContext;
import com.tchalanet.server.featureflags.domain.ports.in.IsFeatureEnabledQuery;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CheckUserPermissionsService implements CheckUserPermissionsUseCase {

  private static final Logger log = LoggerFactory.getLogger(CheckUserPermissionsService.class);

  private final TenantUserDirectoryPort tenantUserDirectoryPort;
  private final PermissionCatalogPort permissionCatalogPort;
  private final IsFeatureEnabledQuery isFeatureEnabledQuery;

  @Override
  public void check(UUID tenantId, UUID userId, Collection<String> permissionCodes)
      throws PermissionsDeniedException {

    // 1) Résoudre membership
    var membership =
        tenantUserDirectoryPort
            .findByTenantAndUser(tenantId, userId)
            .orElseThrow(
                () ->
                    new PermissionsDeniedException(
                        tenantId,
                        userId,
                        Set.copyOf(permissionCodes) // tout manque si pas de membership
                        ));

    // 2) Résoudre permissions effectives
    var rawPermissions = permissionCatalogPort.findPermissionsForRoleHierarchy(membership.roleId());

    var effectiveCodes =
        rawPermissions.stream()
            .filter(code -> isPermissionEnabledForContext(code, null))
            .map(Permission::new)
            .collect(Collectors.toUnmodifiableSet());

    var effective =
        new EffectivePermissions(
            membership.tenantId(), membership.userId(), membership.roleId(), effectiveCodes);

    // 3) Calculer allowed/missing dans le domaine
    CheckPermissionsResult result = effective.check(permissionCodes);

    // 4) Logs
    log.info(
        "Permission check: tenant={} user={} requested={} allowed={} missing= {}",
        tenantId,
        userId,
        permissionCodes,
        result.allowed(),
        result.missingPermissions());

    // 5) Enforcer (TON SNIPPET ICI)
    if (!result.allowed()) {
      throw new PermissionsDeniedException(tenantId, userId, result.missingPermissions());
    }
  }

  private boolean isPermissionEnabledForContext(String permissionCode, FeatureContext ctx) {
    String flagKey = "perm." + permissionCode; // ou "ff.perm." + permissionCode + ".enabled"
    // La logique “default true si flag absent” est côté impl d’IsFeatureEnabledQuery.
    return isFeatureEnabledQuery.isEnabled(flagKey, ctx);
  }
}
