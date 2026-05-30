package com.tchalanet.server.common.context.web;

import static com.tchalanet.server.common.http.TchHeaders.X_DELETED_VISIBILITY;
import static com.tchalanet.server.common.http.TchHeaders.X_TCH_OVERRIDE_REASON;
import static com.tchalanet.server.common.http.TchHeaders.X_TCH_TENANT_OVERRIDE;
import static com.tchalanet.server.common.http.TchHeaders.X_TENANT_ID;

import com.tchalanet.server.common.context.auth.ActorContextResolver;
import com.tchalanet.server.common.context.TchContextBinder;
import com.tchalanet.server.common.context.TchContextProperties;
import com.tchalanet.server.common.context.operational.OperationalContextHeaderParser;
import com.tchalanet.server.common.context.operational.OperationalContextResolver;
import com.tchalanet.server.common.context.tenant.TenantContextResolver;
import com.tchalanet.server.common.security.PlatformPermissions;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 50)
@RequiredArgsConstructor
@Slf4j
public class TchContextFilter extends OncePerRequestFilter {

    private final TchContextProperties contextProperties;
    private final TenantContextResolver tenantContextResolver;
    private final ActorContextResolver actorContextResolver;
    private final TchRequestContextFactory contextFactory;
    private final TchContextBinder contextBinder;
    private final ObjectProvider<OperationalContextResolver> operationalContextResolver;

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
                    ? normalize(contextProperties.publicDefaultTenantCode())
                    : null;

            var ctx = contextFactory.create(req, defaultTenantCode, scope);

            if (!ctx.isSuperAdmin() && hasSensitiveOverrideHeaders(req)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "Super-admin override header forbidden");
                return;
            }

            if (ctx.isSuperAdmin()
                && hasTenantOverride(req)
                && StringUtils.isBlank(req.getHeader(X_TCH_OVERRIDE_REASON))) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "Super-admin override reason required");
                return;
            }

            if (ctx.isSuperAdmin()
                && hasTenantOverride(req)
                && !ctx.hasPermissionClaim(PlatformPermissions.TENANT_OVERRIDE)) {
                res.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "Super-admin tenant override permission required");
                return;
            }

            ctx = tenantContextResolver.resolveForScope(req, res, ctx, scope, defaultTenantCode);

            if (ctx == null) {
                return;
            }

            // Bind tenant context immediately after resolution so RLS is active for all
            // subsequent DB queries (actorContextResolver queries tenant_user with RLS,
            // operationalContextResolver queries terminal_binding with RLS).
            contextBinder.bind(req, ctx);

            ctx = actorContextResolver.attachBootstrappedAppUserId(req, res, ctx);

            if (ctx == null) {
                return;
            }

            if (ctx.isSuperAdmin() && ctx.tenantOverridden()) {
                log.info(
                    "tenant_override.active actorKeycloakId={} actorUserId={} targetTenantId={} reason={} requestId={}",
                    ctx.keycloakUserId(),
                    ctx.userUuid(),
                    ctx.effectiveTenantIdOrNull(),
                    ctx.tenantOverrideReason(),
                    ctx.requestId());
            }

            var resolver = operationalContextResolver.getIfAvailable();
            ctx = ctx.withOperationalContext(
                resolver == null
                    ? OperationalContextHeaderParser.parseHint(req::getHeader)
                    : resolver.resolve(ctx, req::getHeader));

            // Re-bind with the fully resolved context (including actor + operational context).
            contextBinder.bind(req, ctx);

            chain.doFilter(req, res);

        } finally {
            contextBinder.clear(req);
        }
    }

    private boolean hasSensitiveOverrideHeaders(HttpServletRequest req) {
        return StringUtils.isNotBlank(req.getHeader(X_TENANT_ID))
            || StringUtils.isNotBlank(req.getHeader(X_TCH_TENANT_OVERRIDE))
            || StringUtils.isNotBlank(req.getHeader(X_DELETED_VISIBILITY));
    }

    private boolean hasTenantOverride(HttpServletRequest req) {
        return StringUtils.isNotBlank(req.getHeader(X_TENANT_ID))
            || StringUtils.isNotBlank(req.getHeader(X_TCH_TENANT_OVERRIDE));
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }

        var trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
