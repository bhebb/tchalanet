package com.tchalanet.server.platform.accesscontrol.internal.web;

import com.tchalanet.server.common.http.TchHeaders;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.TenantUserRoleJpaRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Decides the effective tenant for an {@code APP_USER} request and whether it is a SUPER_ADMIN
 * tenant override. This is the single place that validates tenant access; callers must not derive an
 * effective tenant by reading headers directly.
 *
 * <p>Contract:
 *
 * <ul>
 *   <li>Normal user — the effective tenant always comes from the user's single active membership.
 *       A request header never selects the tenant for a normal user.
 *   <li>SUPER_ADMIN, no override — no effective tenant (platform scope).
 *   <li>SUPER_ADMIN override — the only header-driven tenant selection: used when a super admin
 *       consults another tenant. Requires {@code X-Tch-Tenant-Override} (valid UUID), the
 *       {@code platform.tenant.override} permission, and a non-blank {@code X-Tch-Override-Reason}.
 *       The target tenant's existence/active status is enforced downstream when tenant metadata is
 *       hydrated.
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EffectiveTenantResolver {

    static final String PERMISSION_TENANT_OVERRIDE = "platform.tenant.override";

    private final TenantUserRoleJpaRepository tenantUserRoleRepository;

    /** Effective tenant decision for an APP_USER request. */
    public record EffectiveTenant(TenantId tenantId, boolean tenantOverride) {
        static EffectiveTenant none() {
            return new EffectiveTenant(null, false);
        }
    }

    public EffectiveTenant resolveForAppUser(
        HttpServletRequest request,
        UserId userId,
        boolean superAdmin,
        Set<String> permissionKeys
    ) {
        var overrideHeader = normalize(request.getHeader(TchHeaders.X_TCH_TENANT_OVERRIDE));

        if (overrideHeader != null) {
            return resolveOverride(request, userId, superAdmin, permissionKeys, overrideHeader);
        }

        if (superAdmin) {
            // A SUPER_ADMIN has no tenant by default. They enter a tenant only via an explicit
            // override (above), never from membership.
            return EffectiveTenant.none();
        }

        // Normal user: the tenant comes from the single active membership, never from a header.
        return resolveSingleMembership(userId);
    }

    public EffectiveTenant resolveForAppUser(
        HttpServletRequest request,
        UserId userId,
        boolean superAdmin,
        Set<String> permissionKeys,
        Collection<TenantId> tenantScopeIds
    ) {
        var overrideHeader = normalize(request.getHeader(TchHeaders.X_TCH_TENANT_OVERRIDE));

        if (overrideHeader != null) {
            return resolveOverride(request, userId, superAdmin, permissionKeys, overrideHeader);
        }

        if (superAdmin) {
            return EffectiveTenant.none();
        }

        return resolveSingleMembership(tenantScopeIds);
    }

    private EffectiveTenant resolveOverride(
        HttpServletRequest request,
        UserId userId,
        boolean superAdmin,
        Set<String> permissionKeys,
        String overrideHeader
    ) {
        if (!superAdmin) {
            throw ProblemRest.forbidden("tenant.override_not_super_admin");
        }
        if (permissionKeys == null || !permissionKeys.contains(PERMISSION_TENANT_OVERRIDE)) {
            throw ProblemRest.forbidden("tenant.override_forbidden");
        }
        if (normalize(request.getHeader(TchHeaders.X_TCH_OVERRIDE_REASON)) == null) {
            throw ProblemRest.forbidden("tenant.override_reason_required");
        }

        var target = parseTenantId(overrideHeader, "tenant.override_invalid");
        log.info("tenant_override.requested userId={} targetTenant={}", userId.value(), target.value());
        return new EffectiveTenant(target, true);
    }

    private EffectiveTenant resolveSingleMembership(UserId userId) {
        var tenantIds = tenantUserRoleRepository.findDistinctActiveTenantIdsByUser(userId.value());

        if (tenantIds.isEmpty()) {
            // No tenant membership. Public/platform scope tolerates this; tenant/admin scope is
            // denied later by TchContextFilter's tenant-required guard.
            return EffectiveTenant.none();
        }
        if (tenantIds.size() > 1) {
            throw ProblemRest.forbidden("tenant.ambiguous_membership");
        }
        return new EffectiveTenant(TenantId.of(tenantIds.getFirst()), false);
    }

    private EffectiveTenant resolveSingleMembership(Collection<TenantId> tenantIds) {
        var distinct = tenantIds == null ? Set.<TenantId>of() : new LinkedHashSet<>(tenantIds);

        if (distinct.isEmpty()) {
            return EffectiveTenant.none();
        }
        if (distinct.size() > 1) {
            throw ProblemRest.forbidden("tenant.ambiguous_membership");
        }
        return new EffectiveTenant(distinct.iterator().next(), false);
    }

    private static TenantId parseTenantId(String raw, String errorCode) {
        try {
            return TenantId.of(UUID.fromString(raw));
        } catch (IllegalArgumentException ex) {
            throw ProblemRest.forbidden(errorCode);
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        var trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
