package com.tchalanet.server.core.ledger.api.query.reconciliation;

import com.tchalanet.server.common.types.id.DrawId;

public record LedgerSummaryForDrawRow(
    DrawId drawId,
    long entryCount,
    long saleEntryCount,
    long payoutEntryCount,
    long saleAmountCents,
    long payoutAmountCents,
    String currency
) {}
