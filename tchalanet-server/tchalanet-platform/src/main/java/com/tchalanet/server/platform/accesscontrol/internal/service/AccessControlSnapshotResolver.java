package com.tchalanet.server.platform.accesscontrol.internal.service;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.UserPermissionOverrideJpaEntity;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.RoleAccessRow;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.TenantUserRoleJpaRepository;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.UserPermissionOverrideJpaRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves a compact access snapshot (role codes + effective permission keys) for an AppUser in as
 * few queries as possible.
 *
 * <p>Platform and tenant access are each one batch join query (role ⨝ role_permission), replacing the
 * previous role-id → role-entity lookups and per-role permission loops (N+1). Tenant access then
 * applies user-level GRANT/DENY overrides, with DENY winning.
 */
@Service
@RequiredArgsConstructor
public class AccessControlSnapshotResolver {

  private final TenantUserRoleJpaRepository tenantUserRoleRepository;
  private final UserPermissionOverrideJpaRepository overrideRepository;

  /** Platform-scope roles/permissions for a user. */
  public record PlatformAccess(boolean superAdmin, Set<String> roleCodes, Set<String> permissionKeys) {}

  /** Tenant-scope roles + effective permissions (overrides applied) for a user in a tenant. */
  public record TenantAccess(Set<String> roleCodes, Set<String> permissionKeys) {}

  @Transactional(readOnly = true)
  public PlatformAccess resolvePlatform(UserId userId) {
    var roleCodes = new HashSet<String>();
    var permissionKeys = new HashSet<String>();
    collectRows(tenantUserRoleRepository.findPlatformRoleAccessRows(userId.value()), roleCodes, permissionKeys);
    return new PlatformAccess(
        roleCodes.contains("SUPER_ADMIN"), Set.copyOf(roleCodes), Set.copyOf(permissionKeys));
  }

  @Transactional(readOnly = true)
  public TenantAccess resolveTenant(UserId userId, TenantId tenantId) {
    var roleCodes = new HashSet<String>();
    var permissionKeys = new HashSet<String>();
    collectRows(
        tenantUserRoleRepository.findTenantRoleAccessRows(tenantId.value(), userId.value()),
        roleCodes,
        permissionKeys);

    // User-level overrides: GRANT adds, DENY removes. DENY wins over role grants and explicit GRANTs.
    var overrides = overrideRepository.findActiveByTenantAndUser(tenantId.value(), userId.value());
    for (var o : overrides) {
      if ("GRANT".equals(o.getEffect())) {
        permissionKeys.add(o.getPermissionCode());
      }
    }
    for (var o : overrides) {
      if ("DENY".equals(o.getEffect())) {
        permissionKeys.remove(o.getPermissionCode());
      }
    }

    return new TenantAccess(Set.copyOf(roleCodes), Set.copyOf(permissionKeys));
  }

  private static void collectRows(
      List<RoleAccessRow> rows, Set<String> roleCodes, Set<String> permissionKeys) {
    for (var row : rows) {
      roleCodes.add(row.getRoleCode());
      if (row.getPermissionCode() != null) {
        permissionKeys.add(row.getPermissionCode());
      }
    }
  }
}
