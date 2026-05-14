package com.tchalanet.server.common.context.tenant;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.ApiScopeResolver;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.types.id.TenantId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TenantContextResolver {

    private final TenantContextLookup tenantLookup;

    public TchRequestContext resolveForScope(
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

    private TchRequestContext resolvePublicTenant(TchRequestContext ctx, String defaultTenantCode) {
        if (ctx.tenantIdSafe() != null) {
            return ctx;
        }

        var code = normalize(ctx.effectiveTenantCode());

        if (StringUtils.isBlank(code)) {
            code = normalize(defaultTenantCode);
        }

        if (StringUtils.isBlank(code)) {
            log.debug("TchContextFilter: no default tenant configured for PUBLIC request");
            return ctx;
        }

        var tenantContextInfo = resolveTenantContext(code);

        if (tenantContextInfo.isEmpty()) {
            log.warn("TchContextFilter: default/public tenant could not be resolved codeOrUuid={}", code);
            return ctx;
        }

        return ctx.withTenantContext(tenantContextInfo.get());
    }

    private TchRequestContext requireAndResolveTenant(HttpServletResponse res, TchRequestContext ctx)
        throws IOException {

        var code = normalize(ctx.effectiveTenantCode());

        if (StringUtils.isBlank(code)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Tenant required");
            return null;
        }

        var tenantContextInfo = resolveTenantContext(code);

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

        return resolveTenantContext(code)
            .map(ctx::withTenantContext)
            .orElse(ctx);
    }

    private Optional<TenantContextInfo> resolveTenantContext(String codeOrUuid) {
        var trimmed = normalize(codeOrUuid);

        if (StringUtils.isBlank(trimmed)) {
            return Optional.empty();
        }

        try {
            var uuid = UUID.fromString(trimmed);
            return tenantLookup.findById(TenantId.of(uuid));
        } catch (IllegalArgumentException ignored) {
            // Not a UUID. Resolve as tenant code below.
        }

        return tenantLookup.findByCode(trimmed);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }

        var trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
