package com.tchalanet.server.core.accesscontrol.domain.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Permissions effectives d'un utilisateur dans un tenant donné.
 */
public record EffectivePermissions(
    UUID tenantId, UUID userId, UUID roleId, Set<String> permissionCodes) {

    public EffectivePermissions {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
        if (permissionCodes == null) {
            permissionCodes = Collections.emptySet();
        } else {
            permissionCodes = Collections.unmodifiableSet(permissionCodes);
        }
    }

    public boolean has(String permissionCode) {
        return permissionCodes.contains(permissionCode);
    }

    /**
     * Vérifie si l'utilisateur possède toutes les permissions demandées.
     */
    public CheckPermissionsResult check(Collection<String> requestedPermissions) {
        if (requestedPermissions == null || requestedPermissions.isEmpty()) {
            // Rien demandé => autorisé
            return new CheckPermissionsResult(true, Collections.emptySet());
        }

        Set<String> missing =
            requestedPermissions.stream()
                .filter(p -> !permissionCodes.contains(p))
                .collect(Collectors.toUnmodifiableSet());

        boolean allowed = missing.isEmpty();
        return new CheckPermissionsResult(allowed, missing);
    }
}
