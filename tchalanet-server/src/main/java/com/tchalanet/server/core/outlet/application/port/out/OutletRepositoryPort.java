package com.tchalanet.server.core.outlet.application.port.out;

import com.tchalanet.server.core.outlet.domain.model.Outlet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutletRepositoryPort {
    Optional<Outlet> findById(UUID id, UUID tenantId);
    List<Outlet> findByTenantId(UUID tenantId);
}

