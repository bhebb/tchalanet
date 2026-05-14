package com.tchalanet.server.core.sales.api.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.*;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.core.sales.internal.domain.event.TicketPlacedLineEvent;
import com.tchalanet.server.core.sales.internal.domain.model.SaleOrigin;
import com.tchalanet.server.core.sales.internal.domain.model.SalesSessionPostingMode;
import com.tchalanet.server.core.sales.internal.domain.model.TicketSyncStatus;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

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
    SalesSessionPostingMode sessionPostingMode,
    List<TicketPlacedLineEvent> lines
) implements DomainEvent {
  public TicketPlacedEvent {
    Objects.requireNonNull(eventId, "eventId is required");
    Objects.requireNonNull(occurredAt, "occurredAt is required");
    Objects.requireNonNull(tenantId, "tenantId is required");
    Objects.requireNonNull(ticketId, "ticketId is required");
    Objects.requireNonNull(outletId, "outletId is required");
    Objects.requireNonNull(sellerUserId, "sellerUserId is required");
    Objects.requireNonNull(terminalId, "id is required");
    Objects.requireNonNull(sessionId, "sessionId is required");
    Objects.requireNonNull(drawId, "drawId is required");
    Objects.requireNonNull(currency, "currency is required");
    Objects.requireNonNull(saleOrigin, "saleOrigin is required");
    Objects.requireNonNull(syncStatus, "syncStatus is required");
    Objects.requireNonNull(sessionPostingMode, "sessionPostingMode is required");
    if (stakeAmountCents < 0 || feeAmountCents < 0 || totalAmountCents < 0) throw new IllegalArgumentException("amounts must be >= 0");
    if (totalAmountCents != stakeAmountCents + feeAmountCents) throw new IllegalArgumentException("total must equal stake + fee");
    lines = List.copyOf(lines == null ? List.of() : lines);
    if (lines.isEmpty()) throw new IllegalArgumentException("lines is required");
  }
}
