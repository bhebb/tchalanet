package com.tchalanet.server.core.accesscontrol.infra.security;

import com.tchalanet.server.core.accesscontrol.domain.model.TchRole;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.jwt.Jwt;

public final class RoleUtils {

  private static final String ROLES_KEY = "roles";
  private static final String REALM_ACCESS = "realm_access";

  private RoleUtils() {}

  /**
   * v1 simplification: collect roles from Keycloak's realm_access.roles only. Expected structure
   * from front/back: { "realm_access": { "roles": ["SUPER_ADMIN","ADMIN","AGENT"] } } Also supports
   * entries like { "name": "SUPER_ADMIN" }.
   */
  public static Set<String> collectRoles(Jwt jwt) {
    if (jwt == null) return Collections.emptySet();
    Object realmAccess = jwt.getClaim(REALM_ACCESS);
    if (!(realmAccess instanceof Map<?, ?> ra)) return Collections.emptySet();
    Object rs = ra.get(ROLES_KEY);
    if (!(rs instanceof Collection<?> col)) return Collections.emptySet();

    return col.stream()
        .map(RoleUtils::extractRoleNameFromElement)
        .filter(Objects::nonNull)
        .map(RoleUtils::normalize)
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
   * Map raw role names (SUPER_ADMIN, ADMIN, AGENT, ...) to system roles (TchRole). For v1 we only
   * support a small set; unknown roles are returned as custom names via RoleSplit.custom
   */
  public static RoleSplit splitRoles(Collection<String> rawRoles) {
    EnumSet<TchRole> system = EnumSet.noneOf(TchRole.class);
    Set<String> custom = new HashSet<>();
    if (rawRoles == null) return new RoleSplit(system, custom);

    for (String r : rawRoles) {
      if (r == null) continue;
      String up = normalize(r);
      if (up.isEmpty()) continue;
      TchRole mapped = mapToSystemRole(up);
      if (mapped != null) system.add(mapped);
      else custom.add(up);
    }
    return new RoleSplit(system, custom);
  }

  private static TchRole mapToSystemRole(String up) {
    // Add aliases here as needed
    return switch (up) {
      case "SUPER_ADMIN", "SUPERADMIN" -> TchRole.SUPER_ADMIN;
      case "ADMIN", "TENANT_ADMIN" -> TchRole.TENANT_ADMIN;
      case "AGENT", "VENDOR", "VENDEUR" -> TchRole.CASHIER;
      default -> null;
    };
  }

  public static final class RoleSplit {
    public final Set<TchRole> system;
    public final Set<String> custom;

    public RoleSplit(Set<TchRole> system, Set<String> custom) {
      // Freeze collections to make RoleSplit immutable from outside
      this.system = Collections.unmodifiableSet(EnumSet.copyOf(system));
      this.custom = Collections.unmodifiableSet(new HashSet<>(custom));
    }
  }
}
