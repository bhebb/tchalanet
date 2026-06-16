package com.tchalanet.server.common.context.web;

import com.tchalanet.server.common.context.ResolvedAccessContext;
import com.tchalanet.server.common.context.TchContextBinder;
import com.tchalanet.server.common.context.TchContextProperties;
import com.tchalanet.server.common.context.TchContextRequestAttributes;
import com.tchalanet.server.common.context.auth.ActorContextResolver;
import com.tchalanet.server.common.context.operational.OperationalContextHeaderParser;
import com.tchalanet.server.common.context.operational.OperationalContextResolver;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.context.tenant.TenantContextResolver;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.tchalanet.server.common.http.TchHeaders.X_TCH_OVERRIDE_REASON;

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
        @Nonnull FilterChain chain
    ) throws ServletException, IOException {

        try {
            var resolvedAccess = (ResolvedAccessContext)
                req.getAttribute(TchContextRequestAttributes.RESOLVED_ACCESS);

            if (resolvedAccess != null) {
                handleResolvedAccess(req, res, chain, resolvedAccess);
                return;
            }

            handlePublicOrLegacy(req, res, chain);

        } finally {
            contextBinder.clear(req);
        }
    }

    private void handleResolvedAccess(
        HttpServletRequest req,
        HttpServletResponse res,
        FilterChain chain,
        ResolvedAccessContext resolvedAccess
    ) throws ServletException, IOException {

        var scope = ApiScopeResolver.resolve(req);


        if (resolvedAccess.tenantOverride()
            && StringUtils.isBlank(req.getHeader(X_TCH_OVERRIDE_REASON))) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Super-admin override reason required");
            return;
        }

        var ctx = contextFactory.createFromResolvedAccess(req, scope, resolvedAccess);

        // Hydrate tenant metadata only (code/timezone/currency). Tenant access was already
        // decided by AccessResolutionStep; this does not re-resolve or validate membership.
        ctx = tenantContextResolver.hydrateResolvedTenant(res, ctx);

        if (ctx == null) {
            return;
        }

        // Bind early so RLS is active before operational DB lookups.
        contextBinder.bind(req, ctx);

        var resolver = operationalContextResolver.getIfAvailable();
        ctx = ctx.withOperationalContext(
            resolver == null
                ? OperationalContextHeaderParser.parseHint(req::getHeader)
                : resolver.resolve(ctx, req::getHeader)
        );

        contextBinder.bind(req, ctx);

        chain.doFilter(req, res);
    }

    private void handlePublicOrLegacy(
        HttpServletRequest req,
        HttpServletResponse res,
        FilterChain chain
    ) throws ServletException, IOException {

        var scope = ApiScopeResolver.resolve(req);

        var defaultTenantCode =
            ApiScopeResolver.allowDefaultTenant(req)
                ? normalize(contextProperties.publicDefaultTenantCode())
                : null;

        var ctx = contextFactory.create(req, defaultTenantCode, scope);

        ctx = tenantContextResolver.resolveForScope(req, res, ctx, scope, defaultTenantCode);

        if (ctx == null) {
            return;
        }

        contextBinder.bind(req, ctx);

        ctx = actorContextResolver.attachBootstrappedAppUserId(req, res, ctx);

        if (ctx == null) {
            return;
        }

        contextBinder.bind(req, ctx);

        chain.doFilter(req, res);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }

        var trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
