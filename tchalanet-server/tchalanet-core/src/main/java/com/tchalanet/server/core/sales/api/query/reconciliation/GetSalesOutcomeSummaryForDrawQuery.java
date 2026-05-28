package com.tchalanet.server.core.sales.api.query.reconciliation;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.DrawId;

public record GetSalesOutcomeSummaryForDrawQuery(
    DrawId drawId
) implements Query<SalesOutcomeSummaryForDrawRow> {}
