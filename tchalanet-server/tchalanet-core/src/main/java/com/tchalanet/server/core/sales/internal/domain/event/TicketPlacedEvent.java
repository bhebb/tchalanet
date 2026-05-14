package com.tchalanet.server.core.sales.internal.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.core.sales.api.model.SaleOrigin;
import com.tchalanet.server.core.sales.api.model.TicketSyncStatus;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;

import java.time.Instant;
import java.util.List;

import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.money.CurrencyCode;

public record TicketPlacedEvent(
    EventId eventId,
    Instant occurredAt,

    TenantId tenantId,
    TicketId ticketId,

    OutletId outletId,
    UserId sellerUserId,
    TerminalId terminalId,
    SalesSessionId sessionId,

    DrawId drawId,
    DrawChannelId drawChannelId,

    long stakeAmountCents,
    long feeAmountCents,
    long totalAmountCents,
    CurrencyCode currency,

    SaleOrigin saleOrigin,
    TicketSyncStatus syncStatus,

    List<TicketPlacedLineEvent> lines
) implements DomainEvent {

    public TicketPlacedEvent {
        if (eventId == null) {
            throw new IllegalArgumentException("eventId is required");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt is required");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (ticketId == null) {
            throw new IllegalArgumentException("ticketId is required");
        }
        if (outletId == null) {
            throw new IllegalArgumentException("outletId is required");
        }
        if (sellerUserId == null) {
            throw new IllegalArgumentException("sellerUserId is required");
        }
        if (terminalId == null) {
            throw new IllegalArgumentException("id is required");
        }
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId is required");
        }
        if (drawId == null) {
            throw new IllegalArgumentException("drawId is required");
        }
        if (drawChannelId == null) {
            throw new IllegalArgumentException("drawChannelId is required");
        }
        if (currency == null) {
            throw new IllegalArgumentException("currency is required");
        }
        if (saleOrigin == null) {
            throw new IllegalArgumentException("saleOrigin is required");
        }
        if (syncStatus == null) {
            throw new IllegalArgumentException("syncStatus is required");
        }
        if (stakeAmountCents < 0) {
            throw new IllegalArgumentException("stakeAmountCents must be >= 0");
        }
        if (feeAmountCents < 0) {
            throw new IllegalArgumentException("feeAmountCents must be >= 0");
        }
        if (totalAmountCents != stakeAmountCents + feeAmountCents) {
            throw new IllegalArgumentException("totalAmountCents must equal stakeAmountCents + feeAmountCents");
        }

        lines = List.copyOf(lines == null ? List.of() : lines);

        if (lines.isEmpty()) {
            throw new IllegalArgumentException("lines is required");
        }
    }

    public long stakeCents() {
        return stakeAmountCents;
    }
}
