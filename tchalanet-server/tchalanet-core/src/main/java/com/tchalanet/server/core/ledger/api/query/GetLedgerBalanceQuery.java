package com.tchalanet.server.core.ledger.api.query;

import java.time.Instant;

public record GetLedgerBalanceQuery(
    String currency,
    Instant occurredFrom,
    Instant occurredTo
) {}
