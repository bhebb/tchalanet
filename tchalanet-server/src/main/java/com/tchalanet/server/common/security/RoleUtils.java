package com.tchalanet.server.common.security;

import com.tchalanet.server.common.domain.TchRole;
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
        .map(String::trim)
        .map(String::toUpperCase)
        .collect(Collectors.toSet());
  }

  private static String extractRoleNameFromElement(Object el) {
    if (el == null) return null;
    if (el instanceof String s) return s;
    if (el instanceof Map<?, ?> m) {
      Object name = m.get("name");
      if (name instanceof String ns) return ns;
      // fallback: try key "role"
      Object role = m.get("role");
      if (role instanceof String rs) return rs;
    }
    return null;
  }

  /**
   * Map raw role names (SUPER_ADMIN, ADMIN, AGENT, ...) to system roles (TchRole). For v1 we only
   * support a small set; unknown roles are returned as custom names via RoleSplit.custom
   */
  public static RoleSplit splitRoles(Collection<String> rawRoles) {
    Set<TchRole> system = EnumSet.noneOf(TchRole.class);
    Set<String> custom = new HashSet<>();
    if (rawRoles == null) return new RoleSplit(system, custom);

    for (String r : rawRoles) {
      if (r == null) continue;
      String up = r.toUpperCase(Locale.ROOT).trim();
      switch (up) {
        case "SUPER_ADMIN", "SUPERADMIN" -> system.add(TchRole.SUPER_ADMIN);
        case "ADMIN", "TENANT_ADMIN" -> system.add(TchRole.ADMIN);
        case "AGENT", "VENDOR", "VENDEUR" -> system.add(TchRole.CASHIER);
        default -> {
          if (!up.isEmpty()) custom.add(up);
        }
      }
    }
    return new RoleSplit(system, custom);
  }

  public static final class RoleSplit {
    public final Set<TchRole> system;
    public final Set<String> custom;

    public RoleSplit(Set<TchRole> system, Set<String> custom) {
      this.system = system;
      this.custom = custom;
    }
  }
}
