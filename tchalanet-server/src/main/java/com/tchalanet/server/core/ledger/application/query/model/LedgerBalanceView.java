package com.tchalanet.server.core.ledger.application.query.model;

import java.time.Instant;

public record LedgerBalanceView(
    long creditCents,
    long debitCents,
    long balanceCents,
    String currency,
    Instant asOf
) {}
