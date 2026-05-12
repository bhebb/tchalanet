package com.tchalanet.server.core.ledger.internal.domain.model;

import com.tchalanet.server.common.types.id.LedgerEntryId;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.TicketId;

import java.util.Objects;
import java.util.UUID;

public record LedgerReference(
    LedgerRefType type,
    UUID id
) {

    public LedgerReference {
        Objects.requireNonNull(type, "type is required");
        Objects.requireNonNull(id, "id is required");
    }

    public static LedgerReference ticketSale(TicketId ticketId) {
        Objects.requireNonNull(ticketId, "ticketId is required");
        return new LedgerReference(LedgerRefType.TICKET_SALE, ticketId.value());
    }

    public static LedgerReference payout(PayoutId payoutId) {
        Objects.requireNonNull(payoutId, "payoutId is required");
        return new LedgerReference(LedgerRefType.PAYOUT, payoutId.value());
    }

    public static LedgerReference reversalOf(LedgerEntryId ledgerEntryId) {
        Objects.requireNonNull(ledgerEntryId, "ledgerEntryId is required");
        return new LedgerReference(LedgerRefType.REVERSAL, ledgerEntryId.value());
    }

    public static LedgerReference cashDeposit(UUID cashMovementId) {
        Objects.requireNonNull(cashMovementId, "cashMovementId is required");
        return new LedgerReference(LedgerRefType.CASH_DEPOSIT, cashMovementId);
    }

    public static LedgerReference cashWithdraw(UUID cashMovementId) {
        Objects.requireNonNull(cashMovementId, "cashMovementId is required");
        return new LedgerReference(LedgerRefType.CASH_WITHDRAW, cashMovementId);
    }
}
