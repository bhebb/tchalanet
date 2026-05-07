package com.tchalanet.server.core.outlet.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.OutletId;

public record GetOutletOperationalContextQuery(OutletId outletId)
    implements Query<OutletOperationalContextView> {}
