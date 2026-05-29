package com.tchalanet.server.core.ledger.api.query.reconciliation;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.DrawId;

public record GetLedgerSummaryForDrawQuery(
    DrawId drawId
) implements Query<LedgerSummaryForDrawRow> {}
