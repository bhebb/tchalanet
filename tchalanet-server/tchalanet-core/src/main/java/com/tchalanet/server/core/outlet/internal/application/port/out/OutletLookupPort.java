package com.tchalanet.server.core.outlet.internal.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.outlet.api.query.OutletSearchCriteria;
import com.tchalanet.server.core.outlet.api.query.OutletSummaryView;
import com.tchalanet.server.core.outlet.internal.domain.model.Outlet;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/** Read-only port used by terminal/integration cross-cuts for outlet validation. */
public interface OutletLookupPort {
    Optional<Outlet> findById(OutletId outletId);
    int countActiveByTenant(TenantId tenantId);
}
