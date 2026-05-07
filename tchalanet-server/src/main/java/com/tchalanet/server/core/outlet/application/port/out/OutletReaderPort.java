package com.tchalanet.server.core.outlet.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.outlet.application.query.model.OutletSearchCriteria;
import com.tchalanet.server.core.outlet.application.query.model.OutletSummaryView;
import com.tchalanet.server.core.outlet.domain.model.Outlet;
import java.util.List;
import java.util.Optional;

public interface OutletReaderPort {
  Optional<Outlet> findById(OutletId id);

  // List all outlets for the current tenant (RLS-scoped at DB level)
  List<Outlet> listByTenant();

  // Paginated search with filters (RLS-scoped)
  TchPage<OutletSummaryView> search(OutletSearchCriteria criteria, TchPageRequest pageRequest);

  default Outlet getRequired(OutletId id) {
    return findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Outlet not found: " + id));
  }
}
