package com.tchalanet.server.common.context;

import com.tchalanet.server.common.constant.CommonConstants;
import com.tchalanet.server.common.context.operational.OperationalContextHint;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.context.tenant.TenantContextInfo;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.CorrelationId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
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
    String originalTenantCode,
    UUID originalTenantUuid,
    String effectiveTenantCode,
    UUID effectiveTenantUuid,
    // @deprecated — use actorType() + externalSubject() instead
    String keycloakUserId,
    UUID appUserId,
    // @deprecated — use roleCodes() instead
    Set<TchRole> systemRoles,
    // @deprecated — use permissionKeys() instead
    Set<String> customRoles,
    Locale locale,
    String requestId,
    String clientIp,
    String userAgent,
    boolean tenantOverridden,
    String tenantOverrideReason,
    String deletedVisibility,
    ApiScope apiScope,
    String idempotencyKey,
    TenantId tenantId,
    ZoneId tenantZoneId,
    Currency tenantCurrency,
    OperationalContextHint operationalContext,
    // Provider-neutral actor fields (Slice 1 — provider-neutral-access-context-v1)
    TchActorType actorType,
    SellerTerminalId sellerTerminalId,
    Set<String> roleCodes,
    Set<String> permissionKeys,
    String externalSubject
) {

    private static final Logger log = LoggerFactory.getLogger(TchRequestContext.class);

    public TchRequestContext {
        roleCodes = roleCodes == null ? Set.of() : Set.copyOf(roleCodes);
        permissionKeys = permissionKeys == null ? Set.of() : Set.copyOf(permissionKeys);
    }

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
     * SUPER_ADMIN > TENANT_ADMIN > OPERATOR > SYSTEM.
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
            originalTenantCode, originalTenantUuid, effectiveTenantCode, uuid,
            keycloakUserId, appUserId, systemRoles, customRoles, locale, requestId,
            clientIp, userAgent, tenantOverridden, tenantOverrideReason, deletedVisibility,
            apiScope, idempotencyKey, tenantId, tenantZoneId, tenantCurrency, operationalContext,
            actorType, sellerTerminalId, roleCodes, permissionKeys, externalSubject
        );
    }

    public TchRequestContext withAppUserId(UUID userId) {
        return new TchRequestContext(
            originalTenantCode, originalTenantUuid, effectiveTenantCode, effectiveTenantUuid,
            keycloakUserId, userId, systemRoles, customRoles, locale, requestId,
            clientIp, userAgent, tenantOverridden, tenantOverrideReason, deletedVisibility,
            apiScope, idempotencyKey, tenantId, tenantZoneId, tenantCurrency, operationalContext,
            actorType, sellerTerminalId, roleCodes, permissionKeys, externalSubject
        );
    }

    /** @deprecated Use {@link #withResolvedAccess} for new provider-neutral code. */
    @Deprecated
    public TchRequestContext withAuthorization(
        Set<TchRole> resolvedSystemRoles, Set<String> resolvedPermissions) {
        return new TchRequestContext(
            originalTenantCode, originalTenantUuid, effectiveTenantCode, effectiveTenantUuid,
            keycloakUserId, appUserId,
            resolvedSystemRoles == null ? Set.of() : Set.copyOf(resolvedSystemRoles),
            resolvedPermissions == null ? Set.of() : Set.copyOf(resolvedPermissions),
            locale, requestId, clientIp, userAgent, tenantOverridden, tenantOverrideReason,
            deletedVisibility, apiScope, idempotencyKey, tenantId, tenantZoneId, tenantCurrency,
            operationalContext, actorType, sellerTerminalId, roleCodes, permissionKeys, externalSubject
        );
    }

    public TchRequestContext withResolvedAccess(
        TchActorType resolvedActorType,
        SellerTerminalId resolvedSellerTerminalId,
        Set<String> resolvedRoleCodes,
        Set<String> resolvedPermissionKeys
    ) {
        return new TchRequestContext(
            originalTenantCode, originalTenantUuid, effectiveTenantCode, effectiveTenantUuid,
            keycloakUserId, appUserId, systemRoles, customRoles, locale, requestId,
            clientIp, userAgent, tenantOverridden, tenantOverrideReason, deletedVisibility,
            apiScope, idempotencyKey, tenantId, tenantZoneId, tenantCurrency, operationalContext,
            resolvedActorType, resolvedSellerTerminalId,
            resolvedRoleCodes == null ? Set.of() : Set.copyOf(resolvedRoleCodes),
            resolvedPermissionKeys == null ? Set.of() : Set.copyOf(resolvedPermissionKeys),
            externalSubject
        );
    }

    public TchRequestContext withIdempotencyKey(String key) {
        return new TchRequestContext(
            originalTenantCode, originalTenantUuid, effectiveTenantCode, effectiveTenantUuid,
            keycloakUserId, appUserId, systemRoles, customRoles, locale, requestId,
            clientIp, userAgent, tenantOverridden, tenantOverrideReason, deletedVisibility,
            apiScope, key, tenantId, tenantZoneId, tenantCurrency, operationalContext,
            actorType, sellerTerminalId, roleCodes, permissionKeys, externalSubject
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

    public SellerTerminalId sellerTerminalIdRequired() {
        if (sellerTerminalId == null) {
            throw ProblemRest.unprocessable(
                "seller_terminal.required: sellerTerminalId is required");
        }
        return sellerTerminalId;
    }

    public TchRequestContext withTenantContext(TenantContextInfo info) {
        return new TchRequestContext(
            originalTenantCode, originalTenantUuid, effectiveTenantCode,
            info.tenantId().value(),     // keep existing UUID field in sync
            keycloakUserId, appUserId, systemRoles, customRoles, locale, requestId,
            clientIp, userAgent, tenantOverridden, tenantOverrideReason, deletedVisibility,
            apiScope, idempotencyKey, info.tenantId(), info.tenantZoneId(), info.currency(),
            operationalContext, actorType, sellerTerminalId, roleCodes, permissionKeys, externalSubject
        );
    }

    /**
     * Factory for a minimal startup/batch tenant context. Use for non-HTTP threads.
     */
    public static TchRequestContext startupTenant(UUID tenantUuid, String requestId) {
        return new TchRequestContext(
            "tchalanet", tenantUuid, "tchalanet", tenantUuid,
            null, null, java.util.EnumSet.noneOf(TchRole.class), Set.of(),
            Locale.getDefault(), requestId == null ? "startup" : requestId,
            "127.0.0.1", null, false, null, "active", ApiScope.TENANT, null,
            TenantId.nullableOf(tenantUuid), ZoneId.systemDefault(),
            Currency.getInstance(CommonConstants.DEFAULT_CURRENCY), null,
            TchActorType.SYSTEM, null, Set.of(), Set.of(), null
        );
    }

    public ApiScope apiScope() {
        return apiScope;
    }

    public boolean isOperator() {
        return systemRoles != null && systemRoles.contains(TchRole.OPERATOR);
    }

    public boolean isTenantAdmin() {
        return systemRoles != null && systemRoles.contains(TchRole.TENANT_ADMIN);
    }

    public boolean isOperationalRole() {
        return isOperator();
    }

    public boolean hasPermissionClaim(String permission) {
        if (permission == null || customRoles == null) {
            return false;
        }

        var normalized = permission.trim().toUpperCase(Locale.ROOT);
        return customRoles.contains(permission)
            || customRoles.contains(normalized);
    }

    public OperationalContextHint operationalContextRequired() {
        if (operationalContext == null) {
            throw ProblemRest.unprocessable("operational_context.required");
        }
        return operationalContext;
    }

    public OperationalContextHint trustedOperationalContextRequired() {
        var context = operationalContextRequired();
        if (!context.trustedForSensitiveOperation()) {
            throw ProblemRest.forbidden("operational_context.untrusted");
        }
        return context;
    }

    public TchRequestContext withOperationalContext(OperationalContextHint operationalContext) {
        return new TchRequestContext(
            originalTenantCode, originalTenantUuid, effectiveTenantCode, effectiveTenantUuid,
            keycloakUserId, appUserId, systemRoles, customRoles, locale, requestId,
            clientIp, userAgent, tenantOverridden, tenantOverrideReason, deletedVisibility,
            apiScope, idempotencyKey, tenantId, tenantZoneId, tenantCurrency, operationalContext,
            actorType, sellerTerminalId, roleCodes, permissionKeys, externalSubject
        );
    }

    public CorrelationId correlationId() {
        return requestId == null || requestId.isBlank()
            ? CorrelationId.newId()
            : CorrelationId.of(requestId);
    }
}
