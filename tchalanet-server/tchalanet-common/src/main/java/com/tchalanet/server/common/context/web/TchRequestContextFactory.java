package com.tchalanet.server.common.context.web;

import static com.tchalanet.server.common.http.TchHeaders.IDEMPOTENCY_KEY;
import static com.tchalanet.server.common.http.TchHeaders.X_DELETED_VISIBILITY;
import static com.tchalanet.server.common.http.TchHeaders.X_FORWARDED_FOR;
import static com.tchalanet.server.common.http.TchHeaders.X_REQUEST_ID;
import static com.tchalanet.server.common.http.TchHeaders.X_TCH_OVERRIDE_REASON;

import com.tchalanet.server.common.context.ResolvedAccessContext;
import com.tchalanet.server.common.context.TchActorType;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.security.TchRole;
import jakarta.servlet.http.HttpServletRequest;
import java.time.ZoneId;
import java.util.Currency;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TchRequestContextFactory {

    /**
     * Provider-neutral protected request path.
     *
     * <p>Used after IdentityBootstrapStep + AccessResolutionStep.
     * Tenant/access facts come from ResolvedAccessContext, not from provider claims.
     */
    public TchRequestContext createFromResolvedAccess(
        HttpServletRequest req,
        ApiScope scope,
        ResolvedAccessContext resolved
    ) {
        var base = baseData(req);

        var effectiveTenantUuid = resolved.effectiveTenantId() == null
            ? null
            : resolved.effectiveTenantId().value();

        var appUserUuid = resolved.appUserId() == null
            ? null
            : resolved.appUserId().value();

        var legacySystemRoles = toLegacyRoles(
            resolved.roleCodes(),
            resolved.actorType()
        );

        var deletedVisibility = resolveDeletedVisibility(
            req,
            resolved.superAdmin()
        );

        var tenantOverrideReason =
            Optional.ofNullable(req.getHeader(X_TCH_OVERRIDE_REASON))
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .orElse(null);

        return new TchRequestContext(
            null,                         // originalTenantCode
            null,                         // originalTenantUuid
            null,                         // effectiveTenantCode
            effectiveTenantUuid,          // effectiveTenantUuid

            null,                         // keycloakUserId legacy
            appUserUuid,                  // appUserId

            legacySystemRoles,            // systemRoles legacy
            resolved.permissionKeys(),    // customRoles legacy compatibility

            base.locale(),
            base.requestId(),
            base.clientIp(),
            base.userAgent(),

            resolved.tenantOverride(),
            tenantOverrideReason,
            deletedVisibility,
            scope,
            base.idempotencyKey(),

            resolved.effectiveTenantId(), // tenantId typed
            null,                         // tenantZoneId resolved later by TenantContextResolver if needed
            null,                         // tenantCurrency resolved later by TenantContextResolver if needed
            null,                         // operationalContext resolved later

            resolved.actorType(),
            resolved.sellerTerminalId(),
            resolved.roleCodes(),
            resolved.permissionKeys(),
            null                          // externalSubject; can be added later from BootstrappedActor if needed
        );
    }

    /**
     * Public/default request path.
     *
     * <p>Used for /public/**, public bootstrap, and legacy unauthenticated flows.
     * Provider claims are not read here; access facts come from the resolved-access pipeline.
     */
    public TchRequestContext createPublic(
        HttpServletRequest req,
        String defaultTenantCode,
        ApiScope scope
    ) {
        var base = baseData(req);

        return new TchRequestContext(
            defaultTenantCode,             // originalTenantCode
            null,                          // originalTenantUuid
            defaultTenantCode,             // effectiveTenantCode
            null,                          // effectiveTenantUuid

            null,                          // keycloakUserId legacy
            null,                          // appUserId

            Set.of(),                      // systemRoles
            Set.of(),                      // customRoles

            base.locale(),
            base.requestId(),
            base.clientIp(),
            base.userAgent(),

            false,                         // tenantOverridden
            null,                          // tenantOverrideReason
            "active",                      // deletedVisibility
            scope,
            base.idempotencyKey(),

            null,                          // tenantId
            null,                          // tenantZoneId
            null,                          // tenantCurrency
            null,                          // operationalContext

            null,                          // actorType: public request has no authenticated actor
            null,                          // sellerTerminalId
            Set.of(),                      // roleCodes
            Set.of(),                      // permissionKeys
            null                           // externalSubject
        );
    }

    /**
     * Legacy-compatible factory kept only if some old public/tenant resolver still calls create(...).
     *
     * <p>It no longer extracts auth from JWT/provider claims. It creates a neutral base context.
     */
    public TchRequestContext create(
        HttpServletRequest req,
        String defaultTenantCode,
        ApiScope scope
    ) {
        return createPublic(req, defaultTenantCode, scope);
    }

    private static Set<TchRole> toLegacyRoles(
        Set<String> roleCodes,
        TchActorType actorType
    ) {
        var roles = new HashSet<TchRole>();

        if (roleCodes != null) {
            for (var roleCode : roleCodes) {
                if (roleCode == null || roleCode.isBlank()) {
                    continue;
                }

                try {
                    roles.add(TchRole.valueOf(roleCode.trim()));
                } catch (IllegalArgumentException ignored) {
                    // Unknown DB role codes are ignored for legacy compatibility.
                    // New authorization should use roleCodes/permissionKeys directly.
                }
            }
        }

        if (actorType == TchActorType.SYSTEM) {
            roles.add(TchRole.SYSTEM);
        }

        return Set.copyOf(roles);
    }

    private BaseRequestData baseData(HttpServletRequest req) {
        var requestId =
            Optional.ofNullable(req.getHeader(X_REQUEST_ID))
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .orElseGet(() -> UUID.randomUUID().toString());

        var clientIp =
            Optional.ofNullable(req.getHeader(X_FORWARDED_FOR))
                .filter(StringUtils::isNotBlank)
                .map(TchRequestContextFactory::firstForwardedIp)
                .orElseGet(req::getRemoteAddr);

        var idempotencyKey =
            Optional.ofNullable(req.getHeader(IDEMPOTENCY_KEY))
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .orElse(null);

        return new BaseRequestData(
            req.getLocale(),
            requestId,
            clientIp,
            req.getHeader("User-Agent"),
            idempotencyKey
        );
    }

    private String resolveDeletedVisibility(HttpServletRequest req, boolean isSuperAdmin) {
        if (!isSuperAdmin) {
            return "active";
        }

        var requested = req.getHeader(X_DELETED_VISIBILITY);

        if (requested == null) {
            return "active";
        }

        var value = requested.trim().toLowerCase(Locale.ROOT);

        return switch (value) {
            case "active", "deleted", "all" -> value;
            default -> "active";
        };
    }

    private static String firstForwardedIp(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        int comma = value.indexOf(',');

        if (comma < 0) {
            return value.trim();
        }

        return value.substring(0, comma).trim();
    }

    private record BaseRequestData(
        Locale locale,
        String requestId,
        String clientIp,
        String userAgent,
        String idempotencyKey
    ) {
    }
}
