package com.tchalanet.server.core.ledger.internal.domain.model;

public enum LedgerRefType {
    CASH_DEPOSIT,
    CASH_WITHDRAW,
    CASH_ADJUSTMENT,
    TICKET_SALE,
    TICKET_CANCEL,
    PAYOUT,
    PAYOUT_REVERSAL,
    REVERSAL;
}
