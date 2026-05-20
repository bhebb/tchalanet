package com.tchalanet.server.core.outlet.api.query;

import com.tchalanet.server.common.types.id.OutletId;

public record ValidatedOutletOperationView(
    OutletId outletId,
    String outletName,
    boolean salesEnabled,
    boolean payoutEnabled,
    boolean offlineSalesEnabled
) {}
