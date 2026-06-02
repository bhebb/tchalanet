package com.tchalanet.server.core.outlet.internal.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.outlet.api.query.OutletSearchCriteria;
import com.tchalanet.server.core.outlet.api.query.OutletSummaryView;
import com.tchalanet.server.core.outlet.internal.domain.model.Outlet;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import com.tchalanet.server.common.exception.TchNotFoundException;
import com.tchalanet.server.common.web.paging.TchPage;

public interface OutletReaderPort {

    Optional<Outlet> findById(OutletId id);

    List<OutletSummaryView> listSummariesByTenant();

    TchPage<OutletSummaryView> search(OutletSearchCriteria criteria, Pageable pageable);

    default Outlet getRequired(OutletId id) {
        // Not-found (incl. RLS-filtered cross-tenant) must be a clean 404, not a 500.
        return findById(id)
            .orElseThrow(() -> new TchNotFoundException(id.toString(), "Outlet not found: "));
    }

    int countActiveByTenant(TenantId tenantId);
}
