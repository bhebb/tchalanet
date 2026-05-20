package com.tchalanet.server.core.outlet.api.command;

import com.tchalanet.server.common.types.id.OutletId;

public record ReopenOutletDayResult(
    OutletId outletId,
    boolean dayReopened,
    boolean salesStillBlocked,
    String salesBlockReason) {}
