package com.tchalanet.server.platform.accesscontrol.internal.service;

import com.tchalanet.server.common.types.enums.TchRole;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.jwt.Jwt;

public final class RoleUtils {

  private static final String ROLES_KEY = "roles";
  private static final String REALM_ACCESS = "realm_access";
  private static final String RESOURCE_ACCESS = "resource_access";

  private RoleUtils() {}

  /**
   * Collect roles from: - Keycloak realm roles: realm_access.roles - Root claim roles: roles (your
   * current token shape) - Optional client roles: resource_access.*.roles
   *
   * <p>Returns normalized role names WITHOUT "ROLE_" prefix (e.g., "TENANT_ADMIN").
   */
  public static Set<String> collectRoles(Jwt jwt) {
    if (jwt == null) return Collections.emptySet();

    Set<String> out = new HashSet<>();

    // 1) realm_access.roles
    Object realmAccess = jwt.getClaim(REALM_ACCESS);
    if (realmAccess instanceof Map<?, ?> ra) {
      Object rs = ra.get(ROLES_KEY);
      if (rs instanceof Collection<?> col) {
        col.stream()
            .map(RoleUtils::extractRoleNameFromElement)
            .filter(Objects::nonNull)
            .map(RoleUtils::normalize)
            .forEach(out::add);
      }
    }

    // 2) root claim roles
    List<String> rootRoles = jwt.getClaimAsStringList(ROLES_KEY);
    if (rootRoles != null) {
      rootRoles.stream().filter(Objects::nonNull).map(RoleUtils::normalize).forEach(out::add);
    }

    // 3) resource_access.*.roles (optional)
    Object resourceAccess = jwt.getClaim(RESOURCE_ACCESS);
    if (resourceAccess instanceof Map<?, ?> res) {
      for (Object entryObj : res.values()) {
        if (entryObj instanceof Map<?, ?> client) {
          Object rs = client.get(ROLES_KEY);
          if (rs instanceof Collection<?> col) {
            col.stream()
                .map(RoleUtils::extractRoleNameFromElement)
                .filter(Objects::nonNull)
                .map(RoleUtils::normalize)
                .forEach(out::add);
          }
        }
      }
    }

    // Strip ROLE_ if a mapper ever sends it (we standardize internally)
    return out.stream()
        .map(r -> r.startsWith("ROLE_") ? r.substring("ROLE_".length()) : r)
        .filter(r -> !r.isBlank())
        .collect(Collectors.toCollection(HashSet::new));
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

  /**
   * Map raw role names ("TENANT_ADMIN", "SUPER_ADMIN", ...) to system roles (TchRole). Unknown
   * roles are returned as custom names via RoleSplit.custom.
   */
  public static RoleSplit splitRoles(Collection<String> rawRoles) {
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

  private static TchRole mapToSystemRole(String up) {
    return switch (up) {
      case "SUPER_ADMIN" -> TchRole.SUPER_ADMIN;
      case "TENANT_ADMIN" -> TchRole.TENANT_ADMIN;
      case "CASHIER" -> TchRole.CASHIER;
      default -> null;
    };
  }

  public static final class RoleSplit {
    public final Set<TchRole> system;
    public final Set<String> custom;

    public RoleSplit(Set<TchRole> system, Set<String> custom) {
      this.system = Collections.unmodifiableSet(EnumSet.copyOf(system));
      this.custom = Collections.unmodifiableSet(new HashSet<>(custom));
    }
  }
}
