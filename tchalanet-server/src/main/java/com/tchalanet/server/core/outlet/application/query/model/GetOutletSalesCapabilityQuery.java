package com.tchalanet.server.core.outlet.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.core.outlet.domain.model.SalesCapability;

public record GetOutletSalesCapabilityQuery(OutletId outletId)
    implements Query<SalesCapability> {}
