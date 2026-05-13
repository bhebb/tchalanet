package com.tchalanet.server.common.context.web;

import static com.tchalanet.server.common.constant.TchHeaders.X_DELETED_VISIBILITY;
import static com.tchalanet.server.common.constant.TchHeaders.X_TCH_OVERRIDE_REASON;
import static com.tchalanet.server.common.constant.TchHeaders.X_TCH_TENANT_OVERRIDE;
import static com.tchalanet.server.common.constant.TchHeaders.X_TENANT_ID;

import com.tchalanet.server.common.context.ActorContextResolver;
import com.tchalanet.server.common.context.ApiScopeResolver;
import com.tchalanet.server.common.context.TchContextBinder;
import com.tchalanet.server.common.context.TchRequestContextFactory;
import com.tchalanet.server.common.context.system.SystemContextProperties;
import com.tchalanet.server.common.context.tenant.TenantContextResolver;
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

    private final SystemContextProperties props;
    private final TenantContextResolver tenantContextResolver;
    private final ActorContextResolver actorContextResolver;
    private final TchRequestContextFactory contextFactory;
    private final TchContextBinder contextBinder;
    private final OperationalContextHeaderParser operationalContextHeaderParser;

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
                    ? normalize(props.tenantCode())
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

            ctx = tenantContextResolver.resolveForScope(req, res, ctx, scope, defaultTenantCode);

            if (ctx == null) {
                return;
            }

            ctx = actorContextResolver.attachBootstrappedAppUserId(req, res, ctx);

            if (ctx == null) {
                return;
            }

            ctx = ctx.withOperationalContext(operationalContextHeaderParser.parseBridge(req));
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

        String trimmed = value.trim();

        return trimmed.isBlank() ? null : trimmed;
    }
}
