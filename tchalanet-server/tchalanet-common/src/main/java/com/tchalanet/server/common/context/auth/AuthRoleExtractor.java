package com.tchalanet.server.common.context.auth;

import com.tchalanet.server.common.security.TchRole;
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
import org.springframework.security.oauth2.jwt.Jwt;

final class AuthRoleExtractor {

  private static final String ROLES_KEY = "roles";
  private static final String REALM_ACCESS = "realm_access";
  private static final String RESOURCE_ACCESS = "resource_access";

  private AuthRoleExtractor() {}

  static Set<String> collectRoles(Jwt jwt) {
    if (jwt == null) return Collections.emptySet();

    Set<String> out = new HashSet<>();

    Object realmAccess = jwt.getClaim(REALM_ACCESS);
    if (realmAccess instanceof Map<?, ?> ra) {
      Object rs = ra.get(ROLES_KEY);
      if (rs instanceof Collection<?> col) {
        col.stream()
            .map(AuthRoleExtractor::extractRoleNameFromElement)
            .filter(Objects::nonNull)
            .map(AuthRoleExtractor::normalize)
            .forEach(out::add);
      }
    }

    List<String> rootRoles = jwt.getClaimAsStringList(ROLES_KEY);
    if (rootRoles != null) {
      rootRoles.stream().filter(Objects::nonNull).map(AuthRoleExtractor::normalize).forEach(out::add);
    }

    Object resourceAccess = jwt.getClaim(RESOURCE_ACCESS);
    if (resourceAccess instanceof Map<?, ?> res) {
      for (Object entryObj : res.values()) {
        if (entryObj instanceof Map<?, ?> client) {
          Object rs = client.get(ROLES_KEY);
          if (rs instanceof Collection<?> col) {
            col.stream()
                .map(AuthRoleExtractor::extractRoleNameFromElement)
                .filter(Objects::nonNull)
                .map(AuthRoleExtractor::normalize)
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

  static RoleSplit splitRoles(Collection<String> rawRoles) {
    EnumSet<TchRole> system = EnumSet.noneOf(TchRole.class);
    Set<String> custom = new HashSet<>();
    if (rawRoles == null) return new RoleSplit(system, custom);

    for (String r : rawRoles) {
      if (r == null) continue;
      String up = normalize(r);
      if (up == null || up.isEmpty()) continue;

      TchRole mapped = mapToSystemRole(up);
      if (mapped != null) system.add(mapped);
      else custom.add(up);
    }
    return new RoleSplit(system, custom);
  }

  private static String normalize(String role) {
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

  static final class RoleSplit {
    final Set<TchRole> system;
    final Set<String> custom;

    RoleSplit(Set<TchRole> system, Set<String> custom) {
      this.system = Collections.unmodifiableSet(EnumSet.copyOf(system));
      this.custom = Collections.unmodifiableSet(new HashSet<>(custom));
    }
  }
}
