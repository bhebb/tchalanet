package com.tchalanet.server.core.outlet.internal.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.outlet.api.query.OutletSearchCriteria;
import com.tchalanet.server.core.outlet.api.query.OutletSummaryView;
import com.tchalanet.server.core.outlet.internal.domain.model.Outlet;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import com.tchalanet.server.common.web.paging.TchPage;

public interface OutletReaderPort {

    Optional<Outlet> findById(OutletId id);

    List<OutletSummaryView> listSummariesByTenant();

    TchPage<OutletSummaryView> search(OutletSearchCriteria criteria, Pageable pageable);

    default Outlet getRequired(OutletId id) {
        return findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Outlet not found: " + id));
    }

    int countActiveByTenant(TenantId tenantId);
}
