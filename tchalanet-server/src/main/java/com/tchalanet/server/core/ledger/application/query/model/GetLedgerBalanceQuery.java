package com.tchalanet.server.core.ledger.application.query.model;

import java.time.Instant;

public record GetLedgerBalanceQuery(
    String currency,
    Instant occurredFrom,
    Instant occurredTo
) {}
