package com.tchalanet.server.core.accesscontrol.application.port.out;

import com.tchalanet.server.core.accesscontrol.domain.model.TenantUserSnapshot;
import java.util.Optional;
import java.util.UUID;

public interface TenantUserDirectoryPort {

    /** Retourne la membership active (rôle unique) pour un user dans un tenant, ou vide si aucune. */
    Optional<TenantUserSnapshot> findActiveMembership(UUID tenantId, UUID userId);
}
