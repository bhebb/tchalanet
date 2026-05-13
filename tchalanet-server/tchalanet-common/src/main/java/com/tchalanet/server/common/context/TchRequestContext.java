package com.tchalanet.server.common.context;

import com.tchalanet.server.common.constant.CommonConstants;
import com.tchalanet.server.common.security.ApiScope;
import com.tchalanet.server.common.types.enums.TchRole;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.error.ProblemRest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;


// Note : @Slf4j n'est pas compatible avec les records Java (annotation processor ne génère pas
// le champ statique `log` sur les records). Logger déclaré explicitement ci-dessous.
public record TchRequestContext(
    String originalTenantCode, // from JWT claim (code like "demo")
    UUID originalTenantUuid, // resolved UUID (may be null)
    String effectiveTenantCode, // effective (may be overridden by SA)
    UUID effectiveTenantUuid, // resolved UUID (may be null)
    String keycloakUserId, // subject from Keycloak JWT (external identity)
    UUID appUserId, // nullable, persisted AppUser UUID, filled after /api/me/bootstrap
    Set<TchRole> systemRoles,
    Set<String> customRoles,
    Locale locale,
    String requestId, // correlation id for logs
    String clientIp,
    String userAgent, // HTTP User-Agent header value (nullable) - moved here
    boolean tenantOverridden, // true if SA override applied
    String tenantOverrideReason,
    String deletedVisibility, // new: requested deleted visibility (active|deleted|all)
    ApiScope apiScope, // NOW typed as enum
    String idempotencyKey,
    // NEW (tenant defaults)
    TenantId tenantId,
    ZoneId tenantZoneId,
    Currency tenantCurrency,
    OperationalRequestContext operationalContext
) {

    private static final Logger log = LoggerFactory.getLogger(TchRequestContext.class);

    /**
     * Return the effective tenant UUID when available, otherwise the original one.
     */
    public UUID tenantUuid() {
        return effectiveTenantUuid != null ? effectiveTenantUuid : originalTenantUuid;
    }

    public TenantId tenantId() {
        return tenantIdSafe();
    }

    /**
     * Compatibility: return the application user id as string when available. Do NOT mix Keycloak
     * subject with app user id. If app user is not known yet, this returns null.
     */
    public UserId userId() {
        return UserId.nullableOf(appUserId);
    }

    /**
     * Return the application user UUID (if present)
     */
    public UUID userUuid() {
        return appUserId;
    }

    /**
     * Helper to convert keycloak subject to UUID when possible (may be non-UUID strings)
     */
    @SuppressWarnings("unused")
    public UUID keycloakAsUuid() {
        try {
            if (keycloakUserId != null) {
                return UUID.fromString(keycloakUserId);
            }
        } catch (IllegalArgumentException e) {
            log.error("Keycloak subject is not a UUID: '{}'", keycloakUserId, e);
        }
        return null;
    }

    public String userAgent() {
        return userAgent;
    }

    /**
     * Rôle principal courant dérivé de systemRoles, avec priorité :
     * SUPER_ADMIN > TENANT_ADMIN > OPERATOR > CASHIER > SYSTEM.
     */
    public TchRole currentRole() {
        if (systemRoles == null || systemRoles.isEmpty()) {
            return null;
        }

        if (systemRoles.contains(TchRole.SUPER_ADMIN)) {
            return TchRole.SUPER_ADMIN;
        }

        if (systemRoles.contains(TchRole.TENANT_ADMIN)) {
            return TchRole.TENANT_ADMIN;
        }

        if (systemRoles.contains(TchRole.OPERATOR)) {
            return TchRole.OPERATOR;
        }

        if (systemRoles.contains(TchRole.CASHIER)) {
            return TchRole.CASHIER;
        }

        if (systemRoles.contains(TchRole.SYSTEM)) {
            return TchRole.SYSTEM;
        }

        return null;
    }

    public String deletedVisibilitySafe() {
        String v = (deletedVisibility == null ? "active" : deletedVisibility.trim().toLowerCase());
        return (v.equals("active") || v.equals("deleted") || v.equals("all")) ? v : "active";
    }

    public boolean isSuperAdmin() {
        return systemRoles != null && systemRoles.contains(TchRole.SUPER_ADMIN);
    }

    /**
     * Convenience builder to return a new TchRequestContext with a different deletedVisibility.
     */

    public TchRequestContext withEffectiveTenantUuid(UUID uuid) {
        return new TchRequestContext(
            originalTenantCode,
            originalTenantUuid,
            effectiveTenantCode,
            uuid,
            keycloakUserId,
            appUserId,
            systemRoles,
            customRoles,
            locale,
            requestId,
            clientIp,
            userAgent,
            tenantOverridden,
            tenantOverrideReason,
            deletedVisibility,
            apiScope,
            idempotencyKey,
            tenantId,
            tenantZoneId,
            tenantCurrency,
            operationalContext
        );
    }

    public TchRequestContext withAppUserId(UUID userId) {
        return new TchRequestContext(
            originalTenantCode,
            originalTenantUuid,
            effectiveTenantCode,
            effectiveTenantUuid,
            keycloakUserId,
            userId,
            systemRoles,
            customRoles,
            locale,
            requestId,
            clientIp,
            userAgent,
            tenantOverridden,
            tenantOverrideReason,
            deletedVisibility,
            apiScope,
            idempotencyKey,
            tenantId,
            tenantZoneId,
            tenantCurrency,
            operationalContext
        );
    }

    // NEW helper
    public TchRequestContext withIdempotencyKey(String key) {
        return new TchRequestContext(
            originalTenantCode,
            originalTenantUuid,
            effectiveTenantCode,
            effectiveTenantUuid,
            keycloakUserId,
            appUserId,
            systemRoles,
            customRoles,
            locale,
            requestId,
            clientIp,
            userAgent,
            tenantOverridden,
            tenantOverrideReason,
            deletedVisibility,
            apiScope,
            key,
            tenantId,
            tenantZoneId,
            tenantCurrency,
            operationalContext
        );
    }

    public TenantId tenantIdSafe() {
        // priorité au champ typé, sinon fallback sur UUID existants
        if (tenantId != null) return tenantId;
        return TenantId.nullableOf(tenantUuid());
    }

    public TenantId effectiveTenantIdOrNull() {
        return tenantIdSafe();
    }

    public TenantId effectiveTenantIdRequired() {
        TenantId effectiveTenantId = effectiveTenantIdOrNull();
        if (effectiveTenantId == null) {
            throw ProblemRest.unprocessable(
                "tenant.required: effective tenant is required");
        }
        return effectiveTenantId;
    }

    public boolean hasTenant() {
        return effectiveTenantIdOrNull() != null;
    }

    public boolean isPlatformScope() {
        return apiScope == ApiScope.PLATFORM;
    }

    /**
     * Retourne le UserId applicatif courant ou lève une exception 422 si l'utilisateur
     * n'a pas encore effectué /api/me/bootstrap (appUserId absent).
     */
    public UserId currentUserIdRequired() {
        if (appUserId == null)
            throw ProblemRest.unprocessable(
                "user.not_bootstrapped: appUserId is required");
        return UserId.of(appUserId);
    }

    public TchRequestContext withTenantContext(TenantContextInfo info) {
        return new TchRequestContext(
            originalTenantCode,
            originalTenantUuid,
            effectiveTenantCode,
            info.tenantId().value(),     // keep existing UUID field in sync
            keycloakUserId,
            appUserId,
            systemRoles,
            customRoles,
            locale,
            requestId,
            clientIp,
            userAgent,
            tenantOverridden,
            tenantOverrideReason,
            deletedVisibility,
            apiScope,
            idempotencyKey,
            info.tenantId(),
            info.tenantZoneId(),
            info.currency(),
            operationalContext
        );
    }

    /**
     * Factory for a minimal startup/batch tenant context. Use for non-HTTP threads.
     */
    public static TchRequestContext startupTenant(UUID tenantUuid, String requestId) {
        return new TchRequestContext(
            "tchalanet",
            tenantUuid,
            "tchalanet",
            tenantUuid,
            null,
            null,
            java.util.EnumSet.noneOf(TchRole.class),
            Set.of(),
            Locale.getDefault(),
            requestId == null ? "startup" : requestId,
            "127.0.0.1",
            null,
            false,
            null,
            "active",
            ApiScope.TENANT,
            null,
            TenantId.nullableOf(tenantUuid),
            ZoneId.systemDefault(),
            Currency.getInstance(CommonConstants.DEFAULT_CURRENCY),
            null
        );
    }

    public ApiScope apiScope() {
        return apiScope;
    }

    public boolean isCashier() {
        return systemRoles != null && systemRoles.contains(TchRole.CASHIER);
    }

    public boolean isOperator() {
        return systemRoles != null && systemRoles.contains(TchRole.OPERATOR);
    }

    public boolean isTenantAdmin() {
        return systemRoles != null && systemRoles.contains(TchRole.TENANT_ADMIN);
    }

    public boolean isOperationalRole() {
        return isCashier() || isOperator();
    }

    public boolean hasPermissionClaim(String permission) {
        if (permission == null || customRoles == null) {
            return false;
        }

        var normalized = permission.trim().toUpperCase(Locale.ROOT);
        return customRoles.contains(permission)
            || customRoles.contains(normalized);
    }

    public OperationalRequestContext operationalContext() {
        return operationalContext == null
            ? OperationalRequestContext.empty()
            : operationalContext;
    }

    public TerminalId terminalIdRequired() {
        return operationalContext().terminalIdRequired();
    }

    public OutletId outletIdRequired() {
        return operationalContext().outletIdRequired();
    }

    public SalesSessionId salesSessionIdRequired() {
        return operationalContext().salesSessionIdRequired();
    }

    public TchRequestContext withOperationalContext(OperationalRequestContext operationalContext) {
        return new TchRequestContext(
            originalTenantCode,
            originalTenantUuid,
            effectiveTenantCode,
            effectiveTenantUuid,
            keycloakUserId,
            appUserId,
            systemRoles,
            customRoles,
            locale,
            requestId,
            clientIp,
            userAgent,
            tenantOverridden,
            tenantOverrideReason,
            deletedVisibility,
            apiScope,
            idempotencyKey,
            tenantId,
            tenantZoneId,
            tenantCurrency,
            operationalContext
        );
    }

    public boolean canUseAdminSelectedOperationalContext() {
        return isSuperAdmin() || isTenantAdmin();
    }

    /**
     * Returns the operational context when it is trusted for a sensitive operation.
     * Throws 403 when the context is missing, the source is not STRONG, or terminalId/outletId
     * are absent. Does not validate terminal-locked / outlet-blocked / session-open.
     */
    public OperationalRequestContext trustedOperationalContextRequired() {
        OperationalRequestContext op = operationalContext();
        if (op.source() == OperationalContextSource.NONE
            || !op.isTrustedForSensitiveOperation()
            || op.terminalId() == null
            || op.outletId() == null) {
            throw ProblemRest.forbidden(
                "operational_context.untrusted: a trusted operational context is required");
        }
        return op;
    }

    public com.tchalanet.server.common.context.operational.PosOperationalContext posOperationalContextRequired() {
        OperationalRequestContext op = operationalContext();

        if (op.source() == OperationalContextSource.NONE
            || op.terminalId() == null
            || op.outletId() == null
            || op.salesSessionId() == null) {
            throw ProblemRest.forbidden(
                "operational_context.missing: POS operational context is required");
        }

        return toPosOperationalContext(op);
    }

    public com.tchalanet.server.common.context.operational.PosOperationalContext
    trustedPosOperationalContextRequired() {
        OperationalRequestContext op = operationalContext();

        if (op.source() == OperationalContextSource.NONE
            || !op.isTrustedForSensitiveOperation()
            || op.terminalId() == null
            || op.outletId() == null
            || op.salesSessionId() == null) {
            throw ProblemRest.forbidden(
                "operational_context.untrusted: a trusted POS operational context is required");
        }

        return toPosOperationalContext(op);
    }

    public com.tchalanet.server.common.context.operational.SellerOperationalContext
    sellerOperationalContextRequired() {
        var pos = trustedPosOperationalContextRequired();

        if (pos.role() != com.tchalanet.server.common.context.operational.OperationalContextRole.SELLER) {
            throw ProblemRest.forbidden(
                "operational_context.wrong_role: seller operational context is required");
        }

        return new com.tchalanet.server.common.context.operational.SellerOperationalContext(
            pos.terminalId(),
            pos.outletId(),
            pos.salesSessionId(),
            pos.sellerUserId(),
            pos.source(),
            pos.trustLevel());
    }

    public com.tchalanet.server.common.context.operational.AdminOperationalContext
    adminOperationalContextRequired() {
        var pos = trustedPosOperationalContextRequired();

        if (pos.role() != com.tchalanet.server.common.context.operational.OperationalContextRole.ADMIN) {
            throw ProblemRest.forbidden(
                "operational_context.wrong_role: admin operational context is required");
        }

        return new com.tchalanet.server.common.context.operational.AdminOperationalContext(
            pos.terminalId(),
            pos.outletId(),
            pos.salesSessionId(),
            userId(),
            pos.source(),
            pos.trustLevel());
    }

    public com.tchalanet.server.common.context.operational.SuperAdminOperationalContext
    superAdminOverrideRequired() {
        if (!isSuperAdmin() || !tenantOverridden || effectiveTenantIdOrNull() == null) {
            throw ProblemRest.forbidden(
                "tenant_override.required: super-admin tenant override is required");
        }

        if (tenantOverrideReason == null || tenantOverrideReason.isBlank()) {
            throw ProblemRest.forbidden(
                "tenant_override.reason_required: override reason is required");
        }

        return new com.tchalanet.server.common.context.operational.SuperAdminOperationalContext(
            effectiveTenantIdRequired(),
            userId(),
            tenantOverrideReason.trim(),
            com.tchalanet.server.common.context.operational.OperationalContextSource.SUPER_ADMIN_OVERRIDE,
            com.tchalanet.server.common.context.operational.TrustLevel.STRONG);
    }

    private com.tchalanet.server.common.context.operational.PosOperationalContext toPosOperationalContext(
        OperationalRequestContext op) {

        return new com.tchalanet.server.common.context.operational.PosOperationalContext(
            op.terminalId(),
            op.outletId(),
            op.salesSessionId(),
            userId(),
            inferOperationalRole(op),
            com.tchalanet.server.common.context.operational.OperationalContextSource.valueOf(
                op.source().name()),
            com.tchalanet.server.common.context.operational.TrustLevel.valueOf(
                op.source().trustLevel().name()));
    }

    private com.tchalanet.server.common.context.operational.OperationalContextRole inferOperationalRole(
        OperationalRequestContext op) {

        if (isSuperAdmin()) {
            return com.tchalanet.server.common.context.operational.OperationalContextRole.SUPER_ADMIN;
        }

        if (isCashier() || isOperator()) {
            return com.tchalanet.server.common.context.operational.OperationalContextRole.SELLER;
        }

        if (isTenantAdmin() || op.source() == OperationalContextSource.ADMIN_SELECTION) {
            return com.tchalanet.server.common.context.operational.OperationalContextRole.ADMIN;
        }

        if (systemRoles != null && systemRoles.contains(TchRole.SYSTEM)) {
            return com.tchalanet.server.common.context.operational.OperationalContextRole.SYSTEM;
        }

        return com.tchalanet.server.common.context.operational.OperationalContextRole.NONE;
    }
}
