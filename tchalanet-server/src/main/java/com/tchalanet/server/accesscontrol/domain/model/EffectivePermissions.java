package com.tchalanet.server.accesscontrol.domain.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Permissions effectives d'un utilisateur dans un tenant donné. */
public record EffectivePermissions(
    UUID tenantId, UUID userId, UUID roleId, Set<Permission> permissionSet) {

  public EffectivePermissions {
    Objects.requireNonNull(tenantId, "tenantId cannot be null");
    Objects.requireNonNull(userId, "userId cannot be null");
    if (permissionSet == null) {
      permissionSet = Collections.emptySet();
    } else {
      permissionSet = Collections.unmodifiableSet(permissionSet);
    }
  }

  public boolean has(String permissionKey) {
    return permissionSet.contains(new Permission(permissionKey));
  }

  /** Vérifie si l'utilisateur possède toutes les permissions demandées. */
  public CheckPermissionsResult check(Collection<String> requestedPermissions) {
    if (requestedPermissions == null || requestedPermissions.isEmpty()) {
      // Rien demandé => autorisé
      return new CheckPermissionsResult(true, Collections.emptySet());
    }

    Set<String> grantedCodes =
        permissionSet.stream().map(Permission::key).collect(Collectors.toUnmodifiableSet());

    Set<String> missing =
        requestedPermissions.stream()
            .filter(p -> !grantedCodes.contains(p))
            .collect(Collectors.toUnmodifiableSet());

    boolean allowed = missing.isEmpty();
    return new CheckPermissionsResult(allowed, missing);
  }
}
