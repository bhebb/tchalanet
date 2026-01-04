package com.tchalanet.server.core.outlet.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.outlet.domain.model.Outlet;
import java.util.Optional;

public interface OutletReaderPort {
  Optional<Outlet> findById(OutletId id, TenantId tenantId);

  // Convenience: return the required outlet or throw IllegalArgumentException
  default Outlet getRequired(OutletId id, TenantId tenantId) {
    return findById(id, tenantId)
        .orElseThrow(() -> new IllegalArgumentException("Outlet not found: " + id));
  }
}
