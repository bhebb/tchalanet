package com.tchalanet.server.core.ledger.api.query;

import com.tchalanet.server.common.bus.Query;
import java.time.Instant;

public record GetLedgerBalanceQuery(
    String currency,
    Instant occurredFrom,
    Instant occurredTo
) implements Query<LedgerBalanceView> {}
