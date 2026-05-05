package com.tchalanet.server.common.security;

import static com.tchalanet.server.common.constant.ContextKeys.BOOTSTRAPPED_APP_USER_ID;
import static com.tchalanet.server.common.constant.ContextKeys.REQUEST_CONTEXT;
import static com.tchalanet.server.common.constant.SecurityClaims.TENANT_CODE;
import static com.tchalanet.server.common.constant.TchHeaders.IDEMPOTENCY_KEY;
import static com.tchalanet.server.common.constant.TchHeaders.X_DELETED_VISIBILITY;
import static com.tchalanet.server.common.constant.TchHeaders.X_FORWARDED_FOR;
import static com.tchalanet.server.common.constant.TchHeaders.X_REQUEST_ID;
import static com.tchalanet.server.common.constant.TchHeaders.X_TENANT_ID;

import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.catalog.tenant.api.model.TenantBootstrapView;
import com.tchalanet.server.common.config.ApiProperties;
import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.TenantContextInfo;
import com.tchalanet.server.common.types.enums.TchRole;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.accesscontrol.infra.security.RoleUtils;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Publishes a per-request context for downstream layers.
 *
 * <p>Tenant resolution rules:
 *
 * <ul>
 *   <li>TENANT routes: tenant is required from JWT tenant_code or super-admin X-Tenant-Id override.
 *   <li>PUBLIC routes: default tenant is resolved from ApiProperties.defaultTenant when allowed.
 *   <li>PLATFORM/SDR routes: tenant is optional.
 * </ul>
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 50)
@RequiredArgsConstructor
@Slf4j
public class TchContextFilter extends OncePerRequestFilter {

    private final ApiProperties props;
    private final TenantCatalog tenantCatalog;

    @Override
    protected void doFilterInternal(
        @Nonnull HttpServletRequest req,
        @Nonnull HttpServletResponse res,
        @Nonnull FilterChain chain)
        throws ServletException, IOException {

        try {
            var scope = ApiScopeResolver.resolve(req);

            var defaultTenantCode =
                ApiScopeResolver.allowDefaultTenant(req)
                    ? normalize(props.defaultTenant())
                    : null;

            var ctx = buildBaseContext(req, defaultTenantCode, scope);

            if (!ctx.isSuperAdmin() && hasSensitiveOverrideHeaders(req)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "Super-admin override header forbidden");
                return;
            }

            ctx = resolveTenantForScope(req, res, ctx, scope, defaultTenantCode);

            if (ctx == null) {
                return;
            }

            ctx = attachBootstrappedAppUserId(req, res, ctx);

            if (ctx == null) {
                return;
            }
            log.info(
                "TchContextFilter SET path={} thread={} scope={} tenantCode={} tenantId={}",
                req.getRequestURI(),
                Thread.currentThread().getName(),
                ctx.apiScope(),
                ctx.effectiveTenantCode(),
                ctx.tenantIdSafe());
            req.setAttribute(REQUEST_CONTEXT, ctx);
            TchContext.set(ctx);
            putMdc(ctx);

            chain.doFilter(req, res);

        } finally {
            log.info(
                "TchContextFilter CLEAR path={} thread={}",
                req.getRequestURI(),
                Thread.currentThread().getName());
            MDC.clear();
            TchContext.clear();
        }
    }

    private TchRequestContext resolveTenantForScope(
        HttpServletRequest req,
        HttpServletResponse res,
        TchRequestContext ctx,
        ApiScope scope,
        String defaultTenantCode)
        throws IOException {

        if (ApiScopeResolver.tenantRequired(req)) {
            return requireAndResolveTenant(res, ctx);
        }

        if (scope == ApiScope.PUBLIC) {
            return resolvePublicTenant(ctx, defaultTenantCode);
        }

        return optionallyResolveTenant(ctx);
    }

    /**
     * PUBLIC routes should use the configured default tenant when present.
     *
     * <p>This is important for public page-model widgets that still need tenant-scoped draw channels,
     * labels, theme, timezone, and feature configuration.
     */
    private TchRequestContext resolvePublicTenant(
        TchRequestContext ctx,
        String defaultTenantCode) {

        if (ctx.tenantIdSafe() != null) {
            return ctx;
        }

        String code = normalize(ctx.effectiveTenantCode());

        if (StringUtils.isBlank(code)) {
            code = normalize(defaultTenantCode);
        }

        if (StringUtils.isBlank(code)) {
            log.debug("TchContextFilter: no default tenant configured for PUBLIC request");
            return ctx;
        }

        Optional<TenantContextInfo> tenantContextInfo = resolveTenantContext(code);

        if (tenantContextInfo.isEmpty()) {
            log.warn(
                "TchContextFilter: default/public tenant could not be resolved codeOrUuid={}",
                code);
            return ctx;
        }

        return ctx.withTenantContext(tenantContextInfo.get());
    }

    private TchRequestContext requireAndResolveTenant(
        HttpServletResponse res,
        TchRequestContext ctx)
        throws IOException {

        var code = normalize(ctx.effectiveTenantCode());

        if (StringUtils.isBlank(code)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Tenant required");
            return null;
        }

        Optional<TenantContextInfo> tenantContextInfo = resolveTenantContext(code);

        if (tenantContextInfo.isEmpty()) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Tenant not found");
            return null;
        }

        return ctx.withTenantContext(tenantContextInfo.get());
    }

    private TchRequestContext optionallyResolveTenant(TchRequestContext ctx) {
        if (ctx.tenantIdSafe() != null) {
            return ctx;
        }

        var code = normalize(ctx.effectiveTenantCode());

        if (StringUtils.isBlank(code)) {
            return ctx;
        }

        Optional<TenantContextInfo> tenantContextInfo = resolveTenantContext(code);

        return tenantContextInfo.map(ctx::withTenantContext).orElse(ctx);
    }

    private TchRequestContext buildBaseContext(
        HttpServletRequest req,
        String defaultTenantCode,
        ApiScope scope) {

        String requestId =
            Optional.ofNullable(req.getHeader(X_REQUEST_ID))
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .orElseGet(() -> UUID.randomUUID().toString());

        String clientIp =
            Optional.ofNullable(req.getHeader(X_FORWARDED_FOR))
                .filter(StringUtils::isNotBlank)
                .map(TchContextFilter::firstForwardedIp)
                .orElseGet(req::getRemoteAddr);

        var idempotencyKey =
            Optional.ofNullable(req.getHeader(IDEMPOTENCY_KEY))
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .orElse(null);

        var locale = req.getLocale();
        String userAgent = req.getHeader("User-Agent");

        AuthData authData = extractAuthData(req, defaultTenantCode);

        String deletedVisibility =
            resolveDeletedVisibility(req, authData.systemRoles.contains(TchRole.SUPER_ADMIN));

        return new TchRequestContext(
            authData.originalTenantCode,
            null,
            authData.effectiveTenantCode,
            null,
            authData.keycloakUserId,
            null,
            authData.systemRoles,
            authData.customRoles,
            locale,
            requestId,
            clientIp,
            userAgent,
            authData.overridden,
            deletedVisibility,
            scope,
            idempotencyKey,
            null,
            null,
            null);
    }

    private AuthData extractAuthData(HttpServletRequest req, String defaultTenantCode) {
        String originalTenantCode = normalize(defaultTenantCode);
        String effectiveTenantCode = normalize(defaultTenantCode);
        String keycloakUserId = null;

        Set<TchRole> systemRoles = new HashSet<>();
        Set<String> customRoles = new HashSet<>();
        boolean overridden = false;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!isJwtAuthentication(auth)) {
            return new AuthData(
                originalTenantCode,
                effectiveTenantCode,
                keycloakUserId,
                overridden,
                systemRoles,
                customRoles);
        }

        Jwt jwt = (Jwt) auth.getPrincipal();

        keycloakUserId = jwt.getClaimAsString("sub");

        String jwtTenant = normalize(jwt.getClaimAsString(TENANT_CODE));

        if (StringUtils.isNotBlank(jwtTenant)) {
            originalTenantCode = jwtTenant;
            effectiveTenantCode = jwtTenant;
        }

        Set<String> rawRoles = RoleUtils.collectRoles(jwt);
        RoleUtils.RoleSplit split = RoleUtils.splitRoles(rawRoles);

        systemRoles.addAll(split.system);
        customRoles.addAll(split.custom);

        if (systemRoles.contains(TchRole.SUPER_ADMIN)) {
            String overrideTenant = normalize(req.getHeader(X_TENANT_ID));

            if (StringUtils.isNotBlank(overrideTenant)) {
                effectiveTenantCode = overrideTenant;
                overridden = true;
            }
        }

        return new AuthData(
            originalTenantCode,
            effectiveTenantCode,
            keycloakUserId,
            overridden,
            systemRoles,
            customRoles);
    }

    private Optional<TenantContextInfo> resolveTenantContext(String codeOrUuid) {
        String trimmed = normalize(codeOrUuid);

        if (StringUtils.isBlank(trimmed)) {
            return Optional.empty();
        }

        try {
            UUID uuid = UUID.fromString(trimmed);

            return tenantCatalog
                .findBootstrapById(TenantId.of(uuid))
                .map(this::toTenantContextInfo);

        } catch (IllegalArgumentException ignored) {
            // Not a UUID. Resolve as tenant code below.
        }

        return tenantCatalog
            .findBootstrapByCode(trimmed)
            .map(this::toTenantContextInfo);
    }

    private TenantContextInfo toTenantContextInfo(TenantBootstrapView view) {
        return new TenantContextInfo(
            view.tenantId(),
            view.currency(),
            view.timezone());
    }

    private TchRequestContext attachBootstrappedAppUserId(
        HttpServletRequest req,
        HttpServletResponse res,
        TchRequestContext ctx)
        throws IOException {

        if (ctx.keycloakUserId() == null || ctx.appUserId() != null) {
            return ctx;
        }

        Object appUserId = req.getAttribute(BOOTSTRAPPED_APP_USER_ID);

        if (appUserId instanceof UUID uuid) {
            return ctx.withAppUserId(uuid);
        }

        if (ctx.apiScope() != ApiScope.PUBLIC) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "User bootstrap required");
            return null;
        }

        return ctx;
    }

    private boolean isJwtAuthentication(Authentication auth) {
        return auth != null
            && auth.isAuthenticated()
            && auth.getPrincipal() instanceof Jwt;
    }

    private String resolveDeletedVisibility(HttpServletRequest req, boolean isSuperAdmin) {
        if (!isSuperAdmin) {
            return "active";
        }

        String requested = req.getHeader(X_DELETED_VISIBILITY);

        if (requested == null) {
            return "active";
        }

        String value = requested.trim().toLowerCase();

        return switch (value) {
            case "active", "deleted", "all" -> value;
            default -> "active";
        };
    }

    private boolean hasSensitiveOverrideHeaders(HttpServletRequest req) {
        return StringUtils.isNotBlank(req.getHeader(X_TENANT_ID))
            || StringUtils.isNotBlank(req.getHeader(X_DELETED_VISIBILITY));
    }

    private void putMdc(TchRequestContext ctx) {
        MDC.put("tenant_original", valueOrDash(ctx.originalTenantCode()));
        MDC.put("tenant_effective", valueOrDash(ctx.effectiveTenantCode()));
        MDC.put("tenant_overridden", String.valueOf(ctx.tenantOverridden()));
        MDC.put("kc_user_id", valueOrDash(ctx.keycloakUserId()));
        MDC.put("reqId", valueOrDash(ctx.requestId()));
        MDC.put("idem", valueOrDash(ctx.idempotencyKey()));
        MDC.put("tenant_uuid", ctx.tenantIdSafe() != null ? ctx.tenantIdSafe().toString() : "-");
        MDC.put("tz", ctx.tenantZoneId() != null ? ctx.tenantZoneId().getId() : "-");
        MDC.put("ccy", ctx.tenantCurrency() != null ? ctx.tenantCurrency().getCurrencyCode() : "-");
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

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isBlank() ? null : trimmed;
    }

    private static String valueOrDash(String value) {
        return value != null ? value : "-";
    }

    private record AuthData(
        String originalTenantCode,
        String effectiveTenantCode,
        String keycloakUserId,
        boolean overridden,
        Set<TchRole> systemRoles,
        Set<String> customRoles) {}
}
