package com.tchalanet.server.common.security;

import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.catalog.tenant.api.model.TenantBootstrapView;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.TenantContextInfo;
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

  private final TenantCatalog tenantCatalog;

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

  /**
   * PUBLIC routes should use the configured default tenant when present.
   *
   * <p>This is important for public page-model widgets that still need tenant-scoped draw channels,
   * labels, theme, timezone, and feature configuration.
   */
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

  private Optional<TenantContextInfo> resolveTenantContext(String codeOrUuid) {
    String trimmed = normalize(codeOrUuid);

    if (StringUtils.isBlank(trimmed)) {
      return Optional.empty();
    }

    try {
      var uuid = UUID.fromString(trimmed);

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

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }

    var trimmed = value.trim();

    return trimmed.isBlank() ? null : trimmed;
  }
}
