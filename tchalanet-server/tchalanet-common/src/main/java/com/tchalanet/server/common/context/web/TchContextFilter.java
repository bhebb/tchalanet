package com.tchalanet.server.common.context.web;

import static com.tchalanet.server.common.http.TchHeaders.X_DELETED_VISIBILITY;
import static com.tchalanet.server.common.http.TchHeaders.X_TCH_OVERRIDE_REASON;
import static com.tchalanet.server.common.http.TchHeaders.X_TCH_TENANT_OVERRIDE;
import static com.tchalanet.server.common.http.TchHeaders.X_TENANT_ID;

import com.tchalanet.server.common.context.ResolvedAccessContext;
import com.tchalanet.server.common.context.TchContextBinder;
import com.tchalanet.server.common.context.TchContextProperties;
import com.tchalanet.server.common.context.TchContextRequestAttributes;
import com.tchalanet.server.common.context.auth.ActorContextResolver;
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

            // Read ResolvedAccessContext set by AccessResolutionFilter (null for public/unauthenticated)
            var resolvedAccess = (ResolvedAccessContext)
                req.getAttribute(TchContextRequestAttributes.RESOLVED_ACCESS);

            var ctx = contextFactory.create(req, defaultTenantCode, scope);

            // Derive superAdmin from DB-resolved state (fallback: JWT-based isSuperAdmin)
            var superAdmin = resolvedAccess != null ? resolvedAccess.superAdmin() : ctx.isSuperAdmin();

            if (!superAdmin && hasSensitiveOverrideHeaders(req)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "Super-admin override header forbidden");
                return;
            }

            if (superAdmin
                && hasTenantOverride(req)
                && StringUtils.isBlank(req.getHeader(X_TCH_OVERRIDE_REASON))) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "Super-admin override reason required");
                return;
            }

            // Use DB-resolved permissions for the override-permission gate
            var hasTenantOverridePerm = resolvedAccess != null
                ? resolvedAccess.permissionKeys().contains(PlatformPermissions.TENANT_OVERRIDE)
                : ctx.hasPermissionClaim(PlatformPermissions.TENANT_OVERRIDE);

            if (superAdmin && hasTenantOverride(req) && !hasTenantOverridePerm) {
                res.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "Super-admin tenant override permission required");
                return;
            }

            // Pre-inject tenant UUID from ResolvedAccessContext so TenantContextResolver can resolve
            // by ID (supports provider-neutral tokens that carry no tenant claim in the JWT).
            if (resolvedAccess != null && resolvedAccess.effectiveTenantId() != null) {
                ctx = ctx.withEffectiveTenantUuid(resolvedAccess.effectiveTenantId().value());
            }

            ctx = tenantContextResolver.resolveForScope(req, res, ctx, scope, defaultTenantCode);

            if (ctx == null) {
                return;
            }

            // Bind tenant context immediately so RLS is active for subsequent DB queries.
            contextBinder.bind(req, ctx);

            if (resolvedAccess != null) {
                // Provider-neutral path: use actor identity from AccessResolutionFilter.
                if (resolvedAccess.isAppUser() && resolvedAccess.appUserId() != null) {
                    ctx = ctx.withAppUserId(resolvedAccess.appUserId().value());
                }
                ctx = ctx.withResolvedAccess(
                    resolvedAccess.actorType(),
                    resolvedAccess.sellerTerminalId(),
                    resolvedAccess.roleCodes(),
                    resolvedAccess.permissionKeys()
                );
                contextBinder.bind(req, ctx);
            } else {
                // Fallback: public or pre-AccessResolutionFilter request (backward compat).
                ctx = actorContextResolver.attachBootstrappedAppUserId(req, res, ctx);
                if (ctx == null) {
                    return;
                }
            }

            if (ctx.tenantOverridden() && !superAdmin) {
                res.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "Super-admin tenant override not confirmed by server authorization");
                return;
            }

            if (superAdmin && ctx.tenantOverridden()) {
                log.info(
                    "tenant_override.active actorType={} actorUserId={} targetTenantId={} reason={} requestId={}",
                    ctx.actorType(),
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

            // Re-bind with the fully resolved context (actor + operational context).
            contextBinder.bind(req, ctx);

            chain.doFilter(req, res);

        } finally {
            contextBinder.clear(req);
        }
    }

    // X-Tenant-Id is the standard provider-neutral tenant selector — not SUPER_ADMIN-only.
    // Only explicit-override and deleted-visibility headers require SUPER_ADMIN.
    private boolean hasSensitiveOverrideHeaders(HttpServletRequest req) {
        return StringUtils.isNotBlank(req.getHeader(X_TCH_TENANT_OVERRIDE))
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
