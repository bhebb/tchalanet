package com.tchalanet.server.filter;

import static com.tchalanet.server.constants.AppConstants.REQUEST_CONTEXT;
import static com.tchalanet.server.constants.AppConstants.TENANT_ID_CLAIMS;

import com.tchalanet.server.config.context.RequestContext;
import com.tchalanet.server.config.properties.ApiProperties;
import com.tchalanet.server.constants.TchRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Builds a per-request RequestContext: - originalTenantId: from JWT claim (fallback to default). -
 * tenantId (effective): original or overridden (SUPER_ADMIN only) via X-Tenant-Id / ?tenantId=. -
 * roles: both system (TchRole) and custom role names preserved. - tenantOverridden: flag for
 * SA-in-tenant mode.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10) // ensure this runs after Spring Security's JWT filter
@RequiredArgsConstructor
public class RequestUserContextFilter extends OncePerRequestFilter {

  private static final String X_REQUEST_ID = "X-Request-Id";
  private static final String X_FORWARDED_FOR = "X-Forwarded-For";

  private final ApiProperties props; // provides defaultTenant(), etc.

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {

    String originalTenant = props.defaultTenant();
    String effectiveTenant = originalTenant;

    String userId = null;
    Locale locale = req.getLocale();
    String requestId =
        Optional.ofNullable(req.getHeader(X_REQUEST_ID)).orElse(UUID.randomUUID().toString());
    String clientIp =
        Optional.ofNullable(req.getHeader(X_FORWARDED_FOR)).orElseGet(req::getRemoteAddr);

    // roles from token
    Set<String> rawRoles = Set.of();
    EnumSet<TchRole> systemRoles = EnumSet.noneOf(TchRole.class);
    Set<String> customRoles = new HashSet<>();
    boolean overridden = false;

    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Jwt jwt) {
      userId = jwt.getSubject();

      // Tenant (original)
      String jwtTenant = jwt.getClaimAsString(TENANT_ID_CLAIMS);
      if (jwtTenant != null && !jwtTenant.isBlank()) {
        originalTenant = jwtTenant;
        effectiveTenant = jwtTenant;
      }

      // Roles (aggregate from common KC places + flat "roles" claim if present)
      rawRoles = collectRoles(jwt);
      RoleSplit split = splitRoles(rawRoles);
      systemRoles = split.system;
      customRoles = split.custom;

      // SA override: header or query param allowed only for SUPER_ADMIN
      boolean isSA = systemRoles.contains(TchRole.SUPER_ADMIN);
      String overrideTenant =
          Optional.ofNullable(req.getHeader("X-Tenant-Id")).orElse(req.getParameter("tenantId"));
      if (isSA && overrideTenant != null && !overrideTenant.isBlank()) {
        effectiveTenant = overrideTenant;
        overridden = true;
      }
    }

    // Build and attach RequestContext (you can extend your record to include customRoles if
    // desired)
    var ctx =
        new RequestContext(
            originalTenant,
            effectiveTenant,
            userId,
            // If your RequestContext already stores Set<String> roles, you can also keep both:
            // here we store only system roles flattened to strings for backward compat:
            // roles as upper strings (system + custom) if your signature expects Set<String>
            // Otherwise, adapt RequestContext to carry both sets explicitly.
            rawRoles.stream()
                .map(t -> TchRole.valueOf(t.toUpperCase(Locale.ROOT)))
                .collect(Collectors.toSet()),
            customRoles,
            locale,
            requestId,
            clientIp,
            overridden);
    // If you extended RequestContext to hold flags/sets, set them there. Otherwise,
    // you can add a separate holder for system/custom roles or expose a RoleService.

    // Attach for controllers/resolvers
    req.setAttribute(REQUEST_CONTEXT, ctx);

    // MDC
    MDC.put("tenant_original", originalTenant != null ? originalTenant : "-");
    MDC.put("tenant_effective", effectiveTenant != null ? effectiveTenant : "-");
    MDC.put("tenant_overridden", String.valueOf(overridden));
    MDC.put("user", userId != null ? userId : "-");
    MDC.put("reqId", requestId);

    try {
      chain.doFilter(req, res);
    } finally {
      MDC.clear();
    }
  }

  /** Collect roles from common Keycloak locations (realm_access, resource_access, flat "roles"). */
  @SuppressWarnings("unchecked")
  private Set<String> collectRoles(Jwt jwt) {
    Object tchObj = jwt.getClaim("tch");
    Set<String> roles = Set.of();

    if (tchObj instanceof Map<?, ?> tchMap) {
      Object rolesObj = tchMap.get("roles");
      if (rolesObj instanceof Collection<?> col) {
        roles =
            col.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .collect(Collectors.toSet());
      }
    }
    return roles;
  }

  /** Split incoming role names into system enum roles vs custom strings. */
  private RoleSplit splitRoles(Set<String> raw) {
    EnumSet<TchRole> system = EnumSet.noneOf(TchRole.class);
    Set<String> custom = new HashSet<>();

    for (String r : raw) {
      String up = r.toUpperCase(Locale.ROOT);
      TchRole mapped = mapToSystemRole(up);
      if (mapped != null) system.add(mapped);
      else custom.add(r);
    }
    return new RoleSplit(system, custom);
  }

  /** Map typical names to TchRole enum; extend aliases as needed. */
  private TchRole mapToSystemRole(String up) {
    // direct enum name
    try {
      return TchRole.valueOf(up);
    } catch (IllegalArgumentException ignore) {
    }

    // common aliases
    switch (up) {
      case "TENANT_ADMIN", "ADMIN_TENANT", "ADMINISTRATOR" -> {
        return TchRole.ADMIN;
      }
      case "VENDEUR", "SELLER" -> {
        return TchRole.CASHIER;
      }
      default -> {
        return null;
      }
    }
  }

  private record RoleSplit(EnumSet<TchRole> system, Set<String> custom) {}
}
