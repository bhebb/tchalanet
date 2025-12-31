package com.tchalanet.server.common.security;

import static com.tchalanet.server.common.constant.ContextKeys.REQUEST_CONTEXT;
import static com.tchalanet.server.common.constant.SecurityClaims.TENANT_ID_CLAIMS;
import static com.tchalanet.server.common.constant.TchHeaders.*;

import com.tchalanet.server.common.bootstrap.tenant.TenantBootstrapLookup;
import com.tchalanet.server.common.config.ApiProperties;
import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.enums.TchRole;
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

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
@Slf4j
public class TchContextFilter extends OncePerRequestFilter {

  private final ApiProperties props;

  private final TenantBootstrapLookup tenantLookup;

  private final TenantCodeToUuidCache tenantCache = new TenantCodeToUuidCache();

  @Override
  protected void doFilterInternal(
      @Nonnull HttpServletRequest req, @Nonnull HttpServletResponse res, @Nonnull FilterChain chain)
      throws ServletException, IOException {

    TchRequestContext ctx = null;
    try {
      var scope = ApiScopeResolver.resolve(req);

      // default tenant allowed ONLY for PUBLIC (per your decision)
      String defaultTenant =
          ApiScopeResolver.allowDefaultTenant(req) ? props.defaultTenant() : null;

      ctx = buildBaseContext(req, defaultTenant);

      // tenant required only for TENANT scope
      if (scope == ApiScope.TENANT) {
        String code = ctx.effectiveTenantCode();
        if (StringUtils.isBlank(code)) {
          res.sendError(HttpServletResponse.SC_FORBIDDEN, "Tenant required");
          return;
        }
        Optional<UUID> uuid = resolveTenantUuid(code.trim());
        if (uuid.isEmpty()) {
          res.sendError(HttpServletResponse.SC_FORBIDDEN, "Tenant not found");
          return;
        }
        ctx = ctx.withEffectiveTenantUuid(uuid.get());
      }

      // OPTIONAL: if PUBLIC has default tenant, you may want to resolve UUID too (to enable RLS for
      // public)
      if (scope == ApiScope.PUBLIC) {
        String code = ctx.effectiveTenantCode();
        if (StringUtils.isNotBlank(code) && ctx.tenantUuid() == null) {
          resolveTenantUuid(code.trim())
              .ifPresent(
                  uuid -> {
                    // note: needs effectively final; easiest is to rebuild below after resolve
                  });
          Optional<UUID> uuid = resolveTenantUuid(code.trim());
          if (uuid.isPresent()) {
            ctx = ctx.withEffectiveTenantUuid(uuid.get());
          }
        }
      }

      // publish context
      req.setAttribute(REQUEST_CONTEXT, ctx);
      TchContext.set(ctx);
      putMdc(ctx);

      chain.doFilter(req, res);

    } finally {
      MDC.clear();
      TchContext.clear();
    }
  }

  private TchRequestContext buildBaseContext(HttpServletRequest req, String defaultTenant) {
    var requestId =
        Optional.ofNullable(req.getHeader(X_REQUEST_ID))
            .orElseGet(() -> UUID.randomUUID().toString());
    var clientIp =
        Optional.ofNullable(req.getHeader(X_FORWARDED_FOR)).orElseGet(req::getRemoteAddr);
    var locale = req.getLocale();

    var authData = extractAuthData(req, defaultTenant);
    var deletedVisibility =
        resolveDeletedVisibility(req, authData.systemRoles.contains(TchRole.SUPER_ADMIN));
    var userAgent = Optional.ofNullable(req.getHeader("User-Agent")).orElse(null);

    return new TchRequestContext(
        authData.originalTenantCode,
        null, // originalTenantUuid resolved later (not needed for now)
        authData.effectiveTenantCode,
        null, // effectiveTenantUuid resolved later
        authData.keycloakUserId,
        null, // appUserId filled later via bootstrap
        authData.systemRoles,
        authData.customRoles,
        locale,
        requestId,
        clientIp,
        userAgent,
        authData.overridden,
        deletedVisibility);
  }

  private AuthData extractAuthData(HttpServletRequest req, String defaultTenant) {
    String originalTenantCode = defaultTenant;
    String effectiveTenantCode = defaultTenant;
    String keycloakUserId = null;

    var systemRoles = new HashSet<TchRole>();
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
    if (jwt == null) {
      return new AuthData(
          originalTenantCode,
          effectiveTenantCode,
          keycloakUserId,
          overridden,
          systemRoles,
          customRoles);
    }

    keycloakUserId = jwt.getSubject();

    // 1) tenant from JWT claim
    String jwtTenant = jwt.getClaimAsString(TENANT_ID_CLAIMS);
    if (StringUtils.isNotBlank(jwtTenant)) {
      originalTenantCode = jwtTenant;
      effectiveTenantCode = jwtTenant;
    }

    // 2) roles
    var rawRoles = RoleUtils.collectRoles(jwt);
    var split = RoleUtils.splitRoles(rawRoles);
    systemRoles.addAll(split.system);
    customRoles = split.custom;

    // 3) SA override
    if (systemRoles.contains(TchRole.SUPER_ADMIN)) {
      String overrideTenant =
          Optional.ofNullable(req.getHeader(X_TENANT_ID)).orElse(req.getParameter("tenantId"));
      if (StringUtils.isNotBlank(overrideTenant)) {
        effectiveTenantCode = overrideTenant.trim();
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

  private boolean isJwtAuthentication(Authentication auth) {
    return auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Jwt;
  }

  private String resolveDeletedVisibility(HttpServletRequest req, boolean isSuperAdmin) {
    if (!isSuperAdmin) return "active";
    String requested = req.getHeader(X_DELETED_VISIBILITY);
    if (StringUtils.isBlank(requested)) requested = req.getParameter("deletedVisibility");
    if (requested == null) return "active";
    String v = requested.trim().toLowerCase();
    return (v.equals("active") || v.equals("deleted") || v.equals("all")) ? v : "active";
  }

  private Optional<UUID> resolveTenantUuid(String codeOrUuid) {
    if (StringUtils.isBlank(codeOrUuid)) return Optional.empty();

    // direct UUID
    try {
      return Optional.of(UUID.fromString(codeOrUuid));
    } catch (IllegalArgumentException ignored) {
    }

    // cache
    var cached = tenantCache.getFresh(codeOrUuid);
    if (cached.isPresent()) return cached;

    // db lookup (rawDataSource bypass RLS)
    Optional<UUID> resolved = tenantLookup.findTenantUuidByCode(codeOrUuid);
    tenantCache.put(codeOrUuid, resolved);
    return resolved;
  }

  private void putMdc(TchRequestContext ctx) {
    MDC.put("tenant_original", valueOrDash(ctx.originalTenantCode()));
    MDC.put("tenant_effective", valueOrDash(ctx.effectiveTenantCode()));
    MDC.put("tenant_overridden", String.valueOf(ctx.tenantOverridden()));
    MDC.put("user", valueOrDash(ctx.keycloakUserId()));
    MDC.put("reqId", valueOrDash(ctx.requestId()));
  }

  private String valueOrDash(String value) {
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
