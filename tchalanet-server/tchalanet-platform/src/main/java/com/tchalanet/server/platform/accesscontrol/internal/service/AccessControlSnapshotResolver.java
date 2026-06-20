package com.tchalanet.server.platform.accesscontrol.internal.service;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.UserPermissionOverrideJpaEntity;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.PlatformUserRoleJpaRepository;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.RoleAccessRow;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.TenantUserRoleJpaRepository;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.UserAccessRow;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.UserAccessSnapshotJpaRepository;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.UserPermissionOverrideJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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

    private final PlatformUserRoleJpaRepository platformUserRoleRepository;
    private final UserAccessSnapshotJpaRepository userAccessSnapshotRepository;
    private final TenantUserRoleJpaRepository tenantUserRoleRepository;
    private final UserPermissionOverrideJpaRepository overrideRepository;

    /**
     * Platform-scope roles/permissions for a user.
     */
    public record PlatformAccess(boolean superAdmin, Set<String> roleCodes, Set<String> permissionKeys) {
    }

    /**
     * Tenant-scope roles + effective permissions (overrides applied) for a user in a tenant.
     */
    public record TenantAccess(Set<String> roleCodes, Set<String> permissionKeys) {
    }

    /**
     * Global DB-backed access snapshot for an app user.
     */
    public record AccessSnapshot(
        UserId userId,
        PlatformAccess platform,
        List<TenantAccessScope> tenantScopes,
        SellerTerminalAccessScope sellerTerminalScope) {
    }

    /**
     * Tenant-scope roles + effective permissions with tenant metadata.
     */
    public record TenantAccessScope(
        TenantId tenantId,
        String tenantCode,
        String tenantName,
        String tenantStatus,
        Set<String> roleCodes,
        Set<String> permissionKeys) {
    }

    /**
     * Reserved for app-user-bound seller terminals once the DB mapping exists.
     */
    public record SellerTerminalAccessScope(
        UUID sellerTerminalId,
        TenantId tenantId,
        String tenantCode,
        String terminalCode,
        String status,
        Set<String> permissionKeys) {
    }

    /**
     * Full access snapshot for runtime/bootstrap only (login, first-login activation, browser
     * refresh). Loads all tenant contexts in a single UNION query — do NOT call this on every
     * API request. Use {@link #resolvePlatform} or {@link #resolveTenant} for request filters.
     */
    @Transactional(readOnly = true)
    public AccessSnapshot resolveUserAccess(UserId userId) {
        var platformRoleCodes = new HashSet<String>();
        var platformPermissionKeys = new HashSet<String>();
        var tenantAccumulators = new LinkedHashMap<UUID, TenantAccessAccumulator>();
        SellerTerminalAccessScope sellerTerminalScope = null;
        var rows = userAccessSnapshotRepository.findUserAccessRows(userId.value());
        for (var row : rows) {
            var scope = row.getScope();
            if ("PLATFORM".equals(scope)) {
                addRow(row, platformRoleCodes, platformPermissionKeys);
            } else if ("TENANT".equals(scope) && row.getTenantId() != null) {
                tenantAccumulators
                    .computeIfAbsent(row.getTenantId(), ignored -> TenantAccessAccumulator.from(row))
                    .add(row);
            } else if ("SELLER_TERMINAL".equals(scope) && row.getSellerTerminalId() != null) {
                sellerTerminalScope = new SellerTerminalAccessScope(
                    row.getSellerTerminalId(),
                    TenantId.nullableOf(row.getTenantId()),
                    row.getTenantCode(),
                    row.getTerminalCode(),
                    row.getSellerTerminalStatus(),
                    Set.of());
            }
        }

        applyOverridesByTenant(tenantAccumulators, overrideRepository.findActiveByUser(userId.value()));

        var platform = new PlatformAccess(
            platformRoleCodes.contains("SUPER_ADMIN"),
            Set.copyOf(platformRoleCodes),
            Set.copyOf(platformPermissionKeys));
        var tenantScopes = tenantAccumulators.values().stream()
            .map(TenantAccessAccumulator::toScope)
            .toList();
        return new AccessSnapshot(userId, platform, List.copyOf(tenantScopes), sellerTerminalScope);
    }

    @Transactional(readOnly = true)
    public PlatformAccess resolvePlatform(UserId userId) {
        var roleCodes = new HashSet<String>();
        var permissionKeys = new HashSet<String>();
        collectRows(platformUserRoleRepository.findPlatformRoleAccessRows(userId.value()), roleCodes, permissionKeys);
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

    private static void addRow(
        UserAccessRow row, Set<String> roleCodes, Set<String> permissionKeys) {
        if (row.getRoleCode() != null) {
            roleCodes.add(row.getRoleCode());
        }
        if (row.getPermissionCode() != null) {
            permissionKeys.add(row.getPermissionCode());
        }
    }

    private static void applyOverridesByTenant(
        Map<UUID, TenantAccessAccumulator> tenants, List<UserPermissionOverrideJpaEntity> overrides) {
        var grants = new ArrayList<UserPermissionOverrideJpaEntity>();
        var denies = new ArrayList<UserPermissionOverrideJpaEntity>();
        for (var override : overrides) {
            if ("GRANT".equals(override.getEffect())) {
                grants.add(override);
            } else if ("DENY".equals(override.getEffect())) {
                denies.add(override);
            }
        }

        for (var grant : grants) {
            var tenant = tenants.get(grant.getTenantId());
            if (tenant != null) {
                tenant.permissionKeys.add(grant.getPermissionCode());
            }
        }
        for (var deny : denies) {
            var tenant = tenants.get(deny.getTenantId());
            if (tenant != null) {
                tenant.permissionKeys.remove(deny.getPermissionCode());
            }
        }
    }

    private record TenantAccessAccumulator(
        TenantId tenantId,
        String tenantCode,
        String tenantName,
        String tenantStatus,
        Set<String> roleCodes,
        Set<String> permissionKeys) {

        static TenantAccessAccumulator from(UserAccessRow row) {
            return new TenantAccessAccumulator(
                TenantId.of(row.getTenantId()),
                row.getTenantCode(),
                row.getTenantName(),
                row.getTenantStatus(),
                new HashSet<>(),
                new HashSet<>());
        }

        void add(UserAccessRow row) {
            addRow(row, roleCodes, permissionKeys);
        }

        TenantAccessScope toScope() {
            return new TenantAccessScope(
                tenantId,
                tenantCode,
                tenantName,
                tenantStatus,
                Set.copyOf(roleCodes),
                Set.copyOf(permissionKeys));
        }
    }
}
