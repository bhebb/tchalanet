package com.tchalanet.server.common.security;

import static com.tchalanet.server.common.constant.ContextKeys.REQUEST_CONTEXT;
import static com.tchalanet.server.common.constant.SecurityClaims.TENANT_CODE;
import static com.tchalanet.server.common.constant.TchHeaders.*;

import com.tchalanet.server.common.bootstrap.tenant.TenantBootstrapLookup;
import com.tchalanet.server.common.bootstrap.user.UserBootstrapLookup;
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

/**
 * Publishes a per-request context (tenant, user, roles, request metadata) for downstream layers.
 *
 * <p>- Reads tenant from JWT claim "tenant_code" (no retro-compat). - Reads roles via RoleUtils
 * (supports Keycloak realm_access.roles + root roles claim). - Stores Keycloak user id = JWT "sub"
 * (jwt.getSubject()). - SUPER_ADMIN can override tenant via X-Tenant-Id (or ?tenantId=...).
 *
 * <p>Order: runs late so SecurityContext is already populated by BearerTokenAuthenticationFilter.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 50)
@RequiredArgsConstructor
@Slf4j
public class TchContextFilter extends OncePerRequestFilter {

  private final ApiProperties props;
  private final TenantBootstrapLookup tenantLookup;
  private final UserBootstrapLookup userLookup;

  private final TenantCodeToUuidCache tenantCache = new TenantCodeToUuidCache();
  private final UserSubToUuidCache userCache = new UserSubToUuidCache();

  @Override
  protected void doFilterInternal(
      @Nonnull HttpServletRequest req, @Nonnull HttpServletResponse res, @Nonnull FilterChain chain)
      throws ServletException, IOException {

    TchRequestContext ctx = null;
    try {
      ApiScope scope = ApiScopeResolver.resolve(req);

      // default tenant allowed ONLY for PUBLIC
      String defaultTenant =
          ApiScopeResolver.allowDefaultTenant(req) ? props.defaultTenant() : null;

      ctx = buildBaseContext(req, defaultTenant);

      // Tenant UUID resolution rules:
      // - TENANT scope: tenant is required (from JWT tenant_code or defaultTenant for PUBLIC-only)
      // - PUBLIC scope: resolve if present (to enable RLS even on public if you want)
      if (scope == ApiScope.TENANT) {
        ctx = requireAndResolveTenant(req, res, ctx);
        if (ctx == null) return; // response already written
      } else if (scope == ApiScope.PUBLIC) {
        ctx = optionallyResolveTenant(ctx);
      } else {
        // ADMIN / PLATFORM: typically tenant comes from JWT claim and is optional at this stage,
        // but if your app requires it for ADMIN, just call requireAndResolveTenant() here too.
        ctx = optionallyResolveTenant(ctx);
      }

      // resolve app user id from keycloak sub (bootstrap lookup bypassing RLS)
      ctx = resolveAppUserId(ctx);

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

  private TchRequestContext requireAndResolveTenant(
      HttpServletRequest req, HttpServletResponse res, TchRequestContext ctx) throws IOException {

    String code = ctx.effectiveTenantCode();
    if (StringUtils.isBlank(code)) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Tenant required");
      return null;
    }

    Optional<UUID> uuid = resolveTenantUuid(code.trim());
    if (uuid.isEmpty()) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Tenant not found");
      return null;
    }
    return ctx.withEffectiveTenantUuid(uuid.get());
  }

  private TchRequestContext optionallyResolveTenant(TchRequestContext ctx) {
    var code = ctx.effectiveTenantCode();
    if (StringUtils.isBlank(code)) return ctx;

    if (ctx.tenantUuid() != null) return ctx;

    var uuid = resolveTenantUuid(code.trim());
    return uuid.map(ctx::withEffectiveTenantUuid).orElse(ctx);
  }

  private TchRequestContext buildBaseContext(HttpServletRequest req, String defaultTenant) {
    var requestId =
        Optional.ofNullable(req.getHeader(X_REQUEST_ID))
            .filter(StringUtils::isNotBlank)
            .orElseGet(() -> UUID.randomUUID().toString());

    var clientIp =
        Optional.ofNullable(req.getHeader(X_FORWARDED_FOR))
            .filter(StringUtils::isNotBlank)
            .orElseGet(req::getRemoteAddr);

    var locale = req.getLocale();
    var userAgent = Optional.ofNullable(req.getHeader("User-Agent")).orElse(null);

    var authData = extractAuthData(req, defaultTenant);

    var deletedVisibility =
        resolveDeletedVisibility(req, authData.systemRoles.contains(TchRole.SUPER_ADMIN));

    return new TchRequestContext(
        authData.originalTenantCode,
        null, // originalTenantUuid resolved later (optional)
        authData.effectiveTenantCode,
        null, // effectiveTenantUuid resolved later
        authData.keycloakUserId, // <-- Keycloak "sub" (UUID string)
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
    // Defaults (PUBLIC only)
    var originalTenantCode = defaultTenant;
    var effectiveTenantCode = defaultTenant;

    // Persist this in your DB (Keycloak Admin API expects this id)
    String keycloakUserId = null;

    Set<TchRole> systemRoles = new HashSet<>();
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

    // 0) Keycloak user id (sub)
    keycloakUserId = jwt.getClaimAsString("sub");

    // 1) tenant from JWT claim (no retro-compat)
    var jwtTenant = jwt.getClaimAsString(TENANT_CODE);
    if (StringUtils.isNotBlank(jwtTenant)) {
      originalTenantCode = jwtTenant.trim();
      effectiveTenantCode = jwtTenant.trim();
    }

    // 2) roles
    Set<String> rawRoles = RoleUtils.collectRoles(jwt);
    RoleUtils.RoleSplit split = RoleUtils.splitRoles(rawRoles);
    systemRoles.addAll(split.system);
    customRoles.addAll(split.custom);

    // 3) SUPER_ADMIN override via header/query
    if (systemRoles.contains(TchRole.SUPER_ADMIN)) {
      var overrideTenant =
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
    var requested = req.getHeader(X_DELETED_VISIBILITY);
    if (StringUtils.isBlank(requested)) requested = req.getParameter("deletedVisibility");
    if (requested == null) return "active";
    var v = requested.trim().toLowerCase();
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

  private TchRequestContext resolveAppUserId(TchRequestContext ctx) {
    if (ctx.keycloakUserId() == null) return ctx;
    if (ctx.appUserId() != null) return ctx;

    // if keycloakUserId is not a UUID string, skip
    UUID sub;
    try {
      sub = UUID.fromString(ctx.keycloakUserId());
    } catch (IllegalArgumentException e) {
      return ctx;
    }

    // cache lookup by sub string
    var cached = userCache.getFresh(sub.toString());
    if (cached.isPresent()) return cached.map(ctx::withAppUserId).orElse(ctx);

    Optional<UUID> resolved = userLookup.findAppUserIdByKeycloakSub(sub);
    userCache.put(sub.toString(), resolved);
    return resolved.map(ctx::withAppUserId).orElse(ctx);
  }

  private void putMdc(TchRequestContext ctx) {
    MDC.put("tenant_original", valueOrDash(ctx.originalTenantCode()));
    MDC.put("tenant_effective", valueOrDash(ctx.effectiveTenantCode()));
    MDC.put("tenant_overridden", String.valueOf(ctx.tenantOverridden()));
    MDC.put("kc_user_id", valueOrDash(ctx.keycloakUserId()));
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
