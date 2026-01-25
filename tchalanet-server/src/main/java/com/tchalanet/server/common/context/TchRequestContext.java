package com.tchalanet.server.common.context;

import com.tchalanet.server.common.types.enums.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import lombok.extern.slf4j.Slf4j;

import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
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
    String deletedVisibility, // new: requested deleted visibility (active|deleted|all)
    String idempotencyKey,
    // NEW (tenant defaults)
    TenantId tenantId,
    ZoneId tenantZoneId,
    Currency tenantCurrency

) {

    /**
     * Return the effective tenant UUID when available, otherwise the original one.
     */
    public UUID tenantUuid() {
        return effectiveTenantUuid != null ? effectiveTenantUuid : originalTenantUuid;
    }

    public TenantId tenantId() {
        return TenantId.nullableOf(tenantUuid());
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
     * Rôle principal courant dérivé de systemRoles, avec priorité : SUPER_ADMIN > TENANT_ADMIN >
     * CASHIER (fallback si aucun des deux premiers).
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

        // Sinon, on considère que l'utilisateur est au moins caissier
        return TchRole.CASHIER;
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
            deletedVisibility,
            idempotencyKey,
            tenantId,
            tenantZoneId,
            tenantCurrency
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
            deletedVisibility,
            idempotencyKey,
            tenantId,
            tenantZoneId,
            tenantCurrency
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
            deletedVisibility,
            key,
            tenantId,
            tenantZoneId,
            tenantCurrency
        );
    }

    public TenantId tenantIdSafe() {
        // priorité au champ typé, sinon fallback sur UUID existants
        if (tenantId != null) return tenantId;
        return TenantId.nullableOf(tenantUuid());
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
            deletedVisibility,
            idempotencyKey,
            info.tenantId(),
            info.tenantZoneId(),
            info.currency()
        );
    }

}
