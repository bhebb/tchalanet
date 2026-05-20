package com.tchalanet.server.core.ledger.internal.domain.model;

import com.tchalanet.server.common.types.id.LedgerEntryId;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.money.CurrencyCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record LedgerEntry(
    LedgerEntryId id,
    TenantId tenantId,
    LedgerReference reference,
    LedgerOperationType operationType,
    long amountCents,
    String currency,
    LedgerDirection direction,
    Instant occurredAt,
    LedgerEntryId reversalOfEntryId,
    String reason
) {
    public LedgerEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(reference, "reference");
        Objects.requireNonNull(operationType, "operationType");
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(occurredAt, "occurredAt");

        if (amountCents <= 0) {
            throw new IllegalArgumentException("amountCents must be positive");
        }
        if (currency.isBlank() || currency.length() != 3) {
            throw new IllegalArgumentException("currency must be ISO-4217 style");
        }
    }

    public static LedgerEntry ticketSale(
        LedgerEntryId id,
        TenantId tenantId,
        TicketId ticketId,
        BigDecimal stakeCents,
        CurrencyCode currency,
        Instant occurredAt
    ) {
        return new LedgerEntry(
            id,
            tenantId,
            LedgerReference.ticketSale(ticketId),
            LedgerOperationType.TICKET_SALE_RECORDED,
            stakeCents.longValue(),
            currency.value(),
            LedgerDirection.CREDIT,
            occurredAt,
            null,
            null
        );
    }

    public static LedgerEntry payoutPaid(
        LedgerEntryId id,
        TenantId tenantId,
        PayoutId payoutId,
        long amountCents,
        String currency,
        Instant occurredAt
    ) {
        return new LedgerEntry(
            id,
            tenantId,
            LedgerReference.payout(payoutId),
            LedgerOperationType.PAYOUT_PAID,
            amountCents,
            currency,
            LedgerDirection.DEBIT,
            occurredAt,
            null,
            null
        );
    }

    public LedgerEntry reversal(
        LedgerEntryId newId,
        Instant occurredAt,
        String reason
    ) {
        var inverse = direction == LedgerDirection.DEBIT
            ? LedgerDirection.CREDIT
            : LedgerDirection.DEBIT;

        return new LedgerEntry(
            newId,
            tenantId,
            reference,
            LedgerOperationType.REVERSAL,
            amountCents,
            currency,
            inverse,
            occurredAt,
            id,
            reason
        );
    }
}
