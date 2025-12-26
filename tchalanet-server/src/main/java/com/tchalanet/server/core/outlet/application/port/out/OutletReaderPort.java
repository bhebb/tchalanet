package com.tchalanet.server.core.outlet.application.port.out;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.core.outlet.domain.model.Outlet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutletRepositoryPort {
    Optional<Outlet> findById(OutletId id, TenantId tenantId);
    List<Outlet> findByTenantId(TenantId tenantId);
}

