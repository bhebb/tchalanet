package com.tchalanet.server.core.outlet.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.outlet.domain.model.Outlet;
import java.util.List;
import java.util.Optional;

public interface OutletReaderPort {
  Optional<Outlet> findById(OutletId id);

  // List all outlets for a tenant (RLS/db-level scoping applies)
  List<Outlet> listByTenant();

  // Convenience: return the required outlet or throw IllegalArgumentException
  default Outlet getRequired(OutletId id) {
    return findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Outlet not found: " + id));
  }
}
