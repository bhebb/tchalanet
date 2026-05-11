package com.tchalanet.server.core.outlet.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.outlet.application.query.model.OutletSearchCriteria;
import com.tchalanet.server.core.outlet.application.query.model.OutletSummaryView;
import com.tchalanet.server.core.outlet.domain.model.Outlet;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface OutletReaderPort {

    Optional<Outlet> findById(OutletId id);

    List<OutletSummaryView> listSummariesByTenant();

    TchPage<OutletSummaryView> search(OutletSearchCriteria criteria, Pageable pageable);

    default Outlet getRequired(OutletId id) {
        return findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Outlet not found: " + id));
    }

    boolean isSalesBlocked(OutletId outletId);
}
