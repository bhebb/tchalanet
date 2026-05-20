package com.tchalanet.server.common.context.auth;

import static com.tchalanet.server.common.context.jwt.SecurityClaims.TENANT_CODE;
import static com.tchalanet.server.common.http.TchHeaders.X_TCH_TENANT_OVERRIDE;
import static com.tchalanet.server.common.http.TchHeaders.X_TENANT_ID;

import com.tchalanet.server.common.security.TchRole;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class AuthContextExtractor {

  private static final String ROLES_KEY = "roles";
  private static final String REALM_ACCESS = "realm_access";
  private static final String RESOURCE_ACCESS = "resource_access";

  public ExtractedAuthContext extract(HttpServletRequest req, String defaultTenantCode) {
    var originalTenantCode = normalize(defaultTenantCode);
    var effectiveTenantCode = normalize(defaultTenantCode);
    String keycloakUserId = null;

    var systemRoles = new HashSet<TchRole>();
    var customRoles = new HashSet<String>();
    boolean overridden = false;

    var auth = SecurityContextHolder.getContext().getAuthentication();

    if (!isJwtAuthentication(auth)) {
      return new ExtractedAuthContext(
          originalTenantCode,
          effectiveTenantCode,
          keycloakUserId,
          overridden,
          systemRoles,
          customRoles);
    }

    var jwt = (Jwt) auth.getPrincipal();

    keycloakUserId = jwt.getClaimAsString("sub");

    var jwtTenant = normalize(jwt.getClaimAsString(TENANT_CODE));

    if (StringUtils.isNotBlank(jwtTenant)) {
      originalTenantCode = jwtTenant;
      effectiveTenantCode = jwtTenant;
    }

    var rawRoles = collectRoles(jwt);
    var split = splitRoles(rawRoles);

    systemRoles.addAll(split.system);
    customRoles.addAll(split.custom);

    if (systemRoles.contains(TchRole.SUPER_ADMIN)) {
      String overrideTenant = normalize(req.getHeader(X_TCH_TENANT_OVERRIDE));

      if (StringUtils.isBlank(overrideTenant)) {
        overrideTenant = normalize(req.getHeader(X_TENANT_ID));
      }

      if (StringUtils.isNotBlank(overrideTenant)) {
        effectiveTenantCode = overrideTenant;
        overridden = true;
      }
    }

    return new ExtractedAuthContext(
        originalTenantCode,
        effectiveTenantCode,
        keycloakUserId,
        overridden,
        systemRoles,
        customRoles);
  }

  private boolean isJwtAuthentication(Authentication auth) {
    return auth != null
        && auth.isAuthenticated()
        && auth.getPrincipal() instanceof Jwt;
  }

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }

    var trimmed = value.trim();

    return trimmed.isBlank() ? null : trimmed;
  }

  private static Set<String> collectRoles(Jwt jwt) {
    if (jwt == null) return Collections.emptySet();

    Set<String> out = new HashSet<>();

    Object realmAccess = jwt.getClaim(REALM_ACCESS);
    if (realmAccess instanceof Map<?, ?> ra) {
      Object rs = ra.get(ROLES_KEY);
      if (rs instanceof Collection<?> col) {
        col.stream()
            .map(AuthContextExtractor::extractRoleNameFromElement)
            .filter(Objects::nonNull)
            .map(AuthContextExtractor::normalizeRole)
            .forEach(out::add);
      }
    }

    List<String> rootRoles = jwt.getClaimAsStringList(ROLES_KEY);
    if (rootRoles != null) {
      rootRoles.stream()
          .filter(Objects::nonNull)
          .map(AuthContextExtractor::normalizeRole)
          .forEach(out::add);
    }

    Object resourceAccess = jwt.getClaim(RESOURCE_ACCESS);
    if (resourceAccess instanceof Map<?, ?> res) {
      for (Object entryObj : res.values()) {
        if (entryObj instanceof Map<?, ?> client) {
          Object rs = client.get(ROLES_KEY);
          if (rs instanceof Collection<?> col) {
            col.stream()
                .map(AuthContextExtractor::extractRoleNameFromElement)
                .filter(Objects::nonNull)
                .map(AuthContextExtractor::normalizeRole)
                .forEach(out::add);
          }
        }
      }
    }

    return out.stream()
        .map(r -> r.startsWith("ROLE_") ? r.substring("ROLE_".length()) : r)
        .filter(r -> !r.isBlank())
        .collect(Collectors.toCollection(HashSet::new));
  }

  private static RoleSplit splitRoles(Collection<String> rawRoles) {
    EnumSet<TchRole> system = EnumSet.noneOf(TchRole.class);
    Set<String> custom = new HashSet<>();
    if (rawRoles == null) return new RoleSplit(system, custom);

    for (String r : rawRoles) {
      if (r == null) continue;
      String up = normalizeRole(r);
      if (up == null || up.isEmpty()) continue;

      TchRole mapped = mapToSystemRole(up);
      if (mapped != null) system.add(mapped);
      else custom.add(up);
    }
    return new RoleSplit(system, custom);
  }

  private static String normalizeRole(String role) {
    return role == null ? null : role.trim().toUpperCase(Locale.ROOT);
  }

  private static String extractRoleNameFromElement(Object el) {
    return switch (el) {
      case String s -> s;
      case Map<?, ?> m -> {
        Object name = m.get("name");
        if (name instanceof String ns) yield ns;
        Object role = m.get("role");
        if (role instanceof String rs) yield rs;
        yield null;
      }
      default -> null;
    };
  }

  private static TchRole mapToSystemRole(String up) {
    return switch (up) {
      case "SUPER_ADMIN" -> TchRole.SUPER_ADMIN;
      case "TENANT_ADMIN" -> TchRole.TENANT_ADMIN;
      case "CASHIER" -> TchRole.CASHIER;
      default -> null;
    };
  }

  private record RoleSplit(Set<TchRole> system, Set<String> custom) {
    private RoleSplit {
      system = Collections.unmodifiableSet(EnumSet.copyOf(system));
      custom = Collections.unmodifiableSet(new HashSet<>(custom));
    }
  }
}
