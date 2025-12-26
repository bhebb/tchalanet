package com.tchalanet.server.common.security;

import static com.tchalanet.server.common.constant.ContextKeys.REQUEST_CONTEXT;
import static com.tchalanet.server.common.constant.SecurityClaims.TENANT_ID_CLAIMS;
import static com.tchalanet.server.common.constant.TchHeaders.X_FORWARDED_FOR;
import static com.tchalanet.server.common.constant.TchHeaders.X_REQUEST_ID;
import static com.tchalanet.server.common.constant.TchHeaders.X_TENANT_ID;

import com.tchalanet.server.common.config.ApiProperties;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.accesscontrol.infra.security.RoleUtils;
import com.tchalanet.server.core.tenant.application.query.handler.ResolveTenantIdByCodeQueryHandler;
import com.tchalanet.server.core.tenant.application.query.model.ResolveTenantIdByCodeQuery;
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
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Builds a per-request RequestContext: - originalTenantCode: from JWT claim (fallback to default).
 * - tenantId (effective): original or overridden (SUPER_ADMIN only) via X-Tenant-Id / ?tenantId=. -
 * roles: both system (TchRole) and custom role names preserved. - tenantOverridden: flag for
 * SA-in-tenant mode.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
@RequiredArgsConstructor
public class RequestUserContextFilter extends OncePerRequestFilter {

  private final ApiProperties props;
  private final ResolveTenantIdByCodeQueryHandler resolveTenantIdByCodeQueryHandler;

  @Override
  protected void doFilterInternal(
      @Nonnull HttpServletRequest req, @Nonnull HttpServletResponse res, @Nonnull FilterChain chain)
      throws ServletException, IOException {

    var ctx = buildRequestContext(req);

    req.setAttribute(REQUEST_CONTEXT, ctx);
    putMdc(ctx);

    try {
      chain.doFilter(req, res);
    } finally {
      MDC.clear();
    }
  }

  // ---------------------------------------------------------------------------
  // Construction du TchRequestContext
  // ---------------------------------------------------------------------------

  private TchRequestContext buildRequestContext(HttpServletRequest req) {
    var defaultTenant = props.defaultTenant();

    var requestId = resolveRequestId(req);
    var clientIp = resolveClientIp(req);
    var locale = req.getLocale();

    var authData = extractAuthData(req, defaultTenant);
    var tenantIds = resolveTenantIds(authData);

    // appUserId inconnu au premier passage (null) — sera rempli par /api/me/bootstrap
    return new TchRequestContext(
        authData.originalTenantCode,
        tenantIds.originalTenantId,
        authData.effectiveTenantCode,
        tenantIds.effectiveTenantId,
        authData.keycloakUserId,
        null,
        authData.systemRoles,
        authData.customRoles,
        locale,
        requestId,
        clientIp,
        authData.overridden);
  }

  private String resolveRequestId(HttpServletRequest req) {
    return Optional.ofNullable(req.getHeader(X_REQUEST_ID))
        .orElseGet(() -> UUID.randomUUID().toString());
  }

  private String resolveClientIp(HttpServletRequest req) {
    return Optional.ofNullable(req.getHeader(X_FORWARDED_FOR)).orElseGet(req::getRemoteAddr);
  }

  // ---------------------------------------------------------------------------
  // Extraction des infos d'authentification / tenant (codes + rôles)
  // ---------------------------------------------------------------------------

  private AuthData extractAuthData(HttpServletRequest req, String defaultTenant) {
    String originalTenantCode = defaultTenant;
    String effectiveTenantCode = defaultTenant;
    String keycloakUserId = null;

    var systemRoles = new HashSet<TchRole>();
    Set<String> customRoles = new HashSet<>();
    boolean overridden = false;

    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (!isJwtAuthentication(auth)) {
      return new AuthData(
          originalTenantCode,
          effectiveTenantCode,
          keycloakUserId,
          overridden,
          systemRoles,
          customRoles);
    }

    var jwt = (Jwt) auth.getPrincipal();
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

    // 1) Tenant provenant du JWT
    var jwtTenant = jwt.getClaimAsString(TENANT_ID_CLAIMS);
    if (jwtTenant != null && !jwtTenant.isBlank()) {
      originalTenantCode = jwtTenant;
      effectiveTenantCode = jwtTenant;
    }

    // 2) Rôles système + custom
    var rawRoles = RoleUtils.collectRoles(jwt);
    var split = RoleUtils.splitRoles(rawRoles);
    systemRoles.addAll(split.system);
    customRoles = split.custom;

    // 3) Override tenant si SUPER_ADMIN
    var isSuperAdmin = systemRoles.contains(TchRole.SUPER_ADMIN);
    if (isSuperAdmin) {
      var overrideTenant =
          Optional.ofNullable(req.getHeader(X_TENANT_ID)).orElse(req.getParameter("tenantId"));
      if (overrideTenant != null && !overrideTenant.isBlank()) {
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

  private boolean isJwtAuthentication(Authentication auth) {
    return auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Jwt;
  }

  // ---------------------------------------------------------------------------
  // Résolution des IDs de tenant à partir des codes
  // ---------------------------------------------------------------------------

  private TenantIds resolveTenantIds(AuthData authData) {
    UUID originalTenantId = resolveTenantIdOrNull(authData.originalTenantCode);
    UUID effectiveTenantId =
        authData.overridden
            ? resolveTenantIdOrNull(authData.effectiveTenantCode)
            : originalTenantId;

    return new TenantIds(originalTenantId, effectiveTenantId);
  }

  private UUID resolveTenantIdOrNull(String tenantCode) {
    if (tenantCode == null || tenantCode.isBlank()) {
      return null;
    }
    return resolveTenantIdByCodeQueryHandler
        .handle(new ResolveTenantIdByCodeQuery(tenantCode))
        .orElse(null);
  }

  // ---------------------------------------------------------------------------
  // MDC
  // ---------------------------------------------------------------------------

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

  // ---------------------------------------------------------------------------
  // Petites structures internes pour clarifier le code
  // ---------------------------------------------------------------------------

  private record AuthData(
      String originalTenantCode,
      String effectiveTenantCode,
      String keycloakUserId,
      boolean overridden,
      Set<TchRole> systemRoles,
      Set<String> customRoles) {}

  private record TenantIds(UUID originalTenantId, UUID effectiveTenantId) {}
}
