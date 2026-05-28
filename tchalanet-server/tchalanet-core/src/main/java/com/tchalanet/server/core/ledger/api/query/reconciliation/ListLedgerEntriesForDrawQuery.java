package com.tchalanet.server.core.ledger.api.query.reconciliation;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.DrawId;
import java.util.List;

public record ListLedgerEntriesForDrawQuery(
    DrawId drawId
) implements Query<List<LedgerEntryForDrawRow>> {}
