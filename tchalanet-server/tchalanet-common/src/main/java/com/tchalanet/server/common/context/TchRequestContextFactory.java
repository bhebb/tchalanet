package com.tchalanet.server.common.context;

import com.tchalanet.server.common.security.ApiScope;
import com.tchalanet.server.common.types.enums.TchRole;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

import static com.tchalanet.server.common.constant.TchHeaders.IDEMPOTENCY_KEY;
import static com.tchalanet.server.common.constant.TchHeaders.X_DELETED_VISIBILITY;
import static com.tchalanet.server.common.constant.TchHeaders.X_FORWARDED_FOR;
import static com.tchalanet.server.common.constant.TchHeaders.X_REQUEST_ID;
import static com.tchalanet.server.common.constant.TchHeaders.X_TCH_OVERRIDE_REASON;

@Component
@RequiredArgsConstructor
public class TchRequestContextFactory {

    private final AuthContextExtractor authContextExtractor;

    public TchRequestContext create(
        HttpServletRequest req,
        String defaultTenantCode,
        ApiScope scope) {

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

        var locale = req.getLocale();
        var userAgent = req.getHeader("User-Agent");

        var authData = authContextExtractor.extract(req, defaultTenantCode);

        var deletedVisibility =
            resolveDeletedVisibility(req, authData.systemRoles().contains(TchRole.SUPER_ADMIN));
        var tenantOverrideReason =
            Optional.ofNullable(req.getHeader(X_TCH_OVERRIDE_REASON))
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .orElse(null);

        return new TchRequestContext(
            authData.originalTenantCode(),
            null,
            authData.effectiveTenantCode(),
            null,
            authData.keycloakUserId(),
            null,
            authData.systemRoles(),
            authData.customRoles(),
            locale,
            requestId,
            clientIp,
            userAgent,
            authData.overridden(),
            tenantOverrideReason,
            deletedVisibility,
            scope,
            idempotencyKey,
            null,
            null,
            null,
            null);
    }

    private String resolveDeletedVisibility(HttpServletRequest req, boolean isSuperAdmin) {
        if (!isSuperAdmin) {
            return "active";
        }

        var requested = req.getHeader(X_DELETED_VISIBILITY);

        if (requested == null) {
            return "active";
        }

        var value = requested.trim().toLowerCase();

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
}
