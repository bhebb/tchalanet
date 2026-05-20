package com.tchalanet.server.core.ledger.api.query;

import java.time.Instant;

public record LedgerBalanceView(
    long creditCents,
    long debitCents,
    long balanceCents,
    String currency,
    Instant asOf
) {}
