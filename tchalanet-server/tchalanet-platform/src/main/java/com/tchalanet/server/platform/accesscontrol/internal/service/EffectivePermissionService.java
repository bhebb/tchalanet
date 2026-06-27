package com.tchalanet.server.platform.accesscontrol.internal.service;

import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.platform.accesscontrol.api.model.request.CheckUserPermissionsRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.request.GetEffectivePermissionsRequest;
import com.tchalanet.server.platform.accesscontrol.api.model.result.CheckUserPermissionsResult;
import com.tchalanet.server.platform.accesscontrol.api.model.view.EffectivePermissionsView;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.adapter.PermissionCatalogAdminAdapter;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.UserPermissionOverrideJpaEntity;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.PlatformUserRoleJpaRepository;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.TenantUserRoleJpaRepository;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.UserPermissionOverrideJpaRepository;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Computes effective permissions for a tenant user and answers permission checks.
 *
 * <p>Effective permissions = active role permissions + user GRANT overrides − user DENY overrides.
 * DENY always wins over role grants and explicit GRANTs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EffectivePermissionService {

  private final TenantUserRoleJpaRepository tenantUserRoleRepository;
  private final PlatformUserRoleJpaRepository platformUserRoleRepository;
  private final PermissionCatalogAdminAdapter permissionCatalogAdminAdapter;
  private final UserPermissionOverrideJpaRepository overrideRepository;

  public CheckUserPermissionsResult checkPermissions(CheckUserPermissionsRequest request) {
    Set<String> required = request.requiredPermissions() == null ? Set.of() : request.requiredPermissions();
    if (required.isEmpty()) {
      return new CheckUserPermissionsResult(true, Set.of());
    }
    var effective = getEffectivePermissions(
        new GetEffectivePermissionsRequest(request.userId(), request.tenantId()));
    var missing = required.stream()
        .filter(p -> !effective.permissionCodes().contains(p))
        .collect(Collectors.toSet());
    return new CheckUserPermissionsResult(missing.isEmpty(), missing);
  }

  @Transactional(readOnly = true)
  public EffectivePermissionsView getEffectivePermissions(GetEffectivePermissionsRequest request) {
    var tenantId = request.tenantId();
    var userId = request.userId();

    if (tenantId == null) {
      return getPlatformEffectivePermissions(userId);
    }

    // 1. Collect permissions from all active roles
    var roleIds = tenantUserRoleRepository
        .findActiveByTenantAndUser(tenantId.value(), userId.value())
        .stream()
        .map(r -> RoleId.of(r.getRoleId()))
        .toList();

    Set<String> rolePermissions = new HashSet<>();
    for (var roleId : roleIds) {
      rolePermissions.addAll(permissionCatalogAdminAdapter.listPermissionCodes(roleId));
    }

    // 2. Apply user-level overrides: GRANT adds, DENY removes (DENY wins)
    var overrides = overrideRepository
        .findActiveByTenantAndUser(tenantId.value(), userId.value());

    Set<String> grants = overrides.stream()
        .filter(o -> "GRANT".equals(o.getEffect()))
        .map(UserPermissionOverrideJpaEntity::getPermissionCode)
        .collect(Collectors.toSet());

    Set<String> denies = overrides.stream()
        .filter(o -> "DENY".equals(o.getEffect()))
        .map(UserPermissionOverrideJpaEntity::getPermissionCode)
        .collect(Collectors.toSet());

    Set<String> effective = new HashSet<>(rolePermissions);
    effective.addAll(grants);
    effective.removeAll(denies); // DENY wins over role grants and explicit GRANTs

    return new EffectivePermissionsView(tenantId, userId, roleIds, Set.copyOf(effective));
  }

  private EffectivePermissionsView getPlatformEffectivePermissions(com.tchalanet.server.common.types.id.UserId userId) {
    var roleIds = platformUserRoleRepository
        .findActivePlatformAssignmentsByUser(userId.value())
        .stream()
        .map(r -> RoleId.of(r.getRoleId()))
        .toList();

    Set<String> effective = new HashSet<>();
    for (var roleId : roleIds) {
      effective.addAll(permissionCatalogAdminAdapter.listPermissionCodes(roleId));
    }

    return new EffectivePermissionsView(null, userId, roleIds, Set.copyOf(effective));
  }
}
