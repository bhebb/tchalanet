package com.tchalanet.server.core.payout.api.query.reconciliation;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.DrawId;

public record GetPayoutSummaryForDrawQuery(
    DrawId drawId
) implements Query<PayoutSummaryForDrawRow> {}
