package com.tchalanet.server.platform.accesscontrol.internal.service;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.auth.ActorAuthorizationContextResolver;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.platform.accesscontrol.api.model.request.GetEffectivePermissionsRequest;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.adapter.PermissionCatalogAdminAdapter;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.AppRoleJpaRepository;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.TenantUserRoleJpaRepository;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DatabaseActorAuthorizationContextResolver
    implements ActorAuthorizationContextResolver {

  private final EffectivePermissionService effectivePermissionService;
  private final AppRoleJpaRepository appRoleRepository;
  private final TenantUserRoleJpaRepository tenantUserRoleRepository;
  private final PermissionCatalogAdminAdapter permissionCatalogAdminAdapter;

  @Override
  @Transactional(readOnly = true)
  public TchRequestContext resolve(TchRequestContext context) {
    var platformRoleIds =
        tenantUserRoleRepository
            .findActivePlatformRoleIdsByUser(context.currentUserIdRequired().value())
            .stream()
            .map(RoleId::of)
            .toList();
    var roleIds = new LinkedHashSet<>(platformRoleIds);
    var permissions = new HashSet<String>();

    if (context.hasTenant()) {
      var effective =
          effectivePermissionService.getEffectivePermissions(
              new GetEffectivePermissionsRequest(
                  context.currentUserIdRequired(), context.effectiveTenantIdRequired()));
      roleIds.addAll(effective.roleIds());
      permissions.addAll(effective.permissionCodes());
    }
    platformRoleIds.forEach(
        roleId -> permissions.addAll(permissionCatalogAdminAdapter.listPermissionCodes(roleId)));

    var systemRoles = EnumSet.noneOf(TchRole.class);
    appRoleRepository.findAllById(roleIds.stream().map(RoleId::value).toList())
        .stream()
        .filter(role -> role.isSystem() && role.isActive())
        .map(role -> parseSystemRole(role.getCode()))
        .filter(java.util.Objects::nonNull)
        .forEach(systemRoles::add);

    return context.withAuthorization(systemRoles, permissions);
  }

  private static TchRole parseSystemRole(String roleCode) {
    if (roleCode == null || roleCode.isBlank()) {
      return null;
    }
    try {
      return TchRole.valueOf(roleCode.trim().toUpperCase(java.util.Locale.ROOT));
    } catch (IllegalArgumentException ignored) {
      return null;
    }
  }
}
