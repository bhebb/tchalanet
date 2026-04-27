package com.tchalanet.server.core.sales.domain.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.enums.BetType;
import com.tchalanet.server.common.types.id.AgentId;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import java.time.Instant;
import java.util.List;
import org.springframework.lang.Nullable;

public record TicketPlacedEvent(
    EventId eventId,             // typed EventId (accessor: eventId())
    Instant occurredAt,
    TenantId tenantId,

    TicketId ticketId,
    OutletId outletId,

    @Nullable AgentId agentId,     // agent (nullable) replaces the former cashierId UUID
    @Nullable TerminalId terminalId, // NEW (optionnel)
    @Nullable SessionId sessionId,

    DrawId drawId,
    @Nullable DrawChannelId drawChannelId, // NEW (optionnel)

    String gameCode,
    long stakeCents,
    String currencyCode,

    List<Line> lines               // NEW
) implements DomainEvent {

  public record Line(
      BetType betType,
      String selectionKeyRaw,
      long stakeCents,
      long potentialPayoutCents,
      @Nullable Short betOption
  ) {}
}
