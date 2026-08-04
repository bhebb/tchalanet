package com.tchalanet.server.core.sales.api.event;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.types.id.CorrelationId;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import com.tchalanet.server.core.sales.api.event.payload.TicketContextPayload;
import com.tchalanet.server.core.sales.api.event.payload.TicketMoneyPayload;
import com.tchalanet.server.core.sales.api.model.origin.TicketSaleChannel;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import java.time.Instant;
import java.util.List;

/**
 * Domain event: a ticket has entered the system.
 *
 * <p>All ticket placements are immediately official sales. A limit decision that cannot be accepted
 * is rejected before this event is emitted.
 *
 * <p>Payload is grouped by concern (context / money / lines / offline) using sibling payload
 * records under {@code .payload}. This keeps the event API stable as new concerns are added (extend
 * the relevant payload record rather than the event signature).
 */
public record TicketPlacedEvent(
    // Envelope
    EventId eventId,
    int schemaVersion,
    Instant occurredAt,
    CorrelationId correlationId,

    // Subject
    TenantId tenantId,
    TicketId ticketId,

    // Placement state
    TicketSaleStatus saleStatus,
    TicketSaleChannel saleChannel,

    // Grouped payloads
    TicketContextPayload context,
    TicketMoneyPayload money,
    List<TicketLinePlacedItem> lines,
    PromotionDecision promotionDecision)
    implements DomainEvent {
  public static final int CURRENT_SCHEMA = 5;

  public TicketPlacedEvent {
    if (saleStatus != TicketSaleStatus.APPROVED) {
      throw new IllegalArgumentException(
          "TicketPlacedEvent must carry APPROVED, got " + saleStatus);
    }
    if (lines == null || lines.isEmpty()) {
      throw new IllegalArgumentException("lines must not be empty");
    }
    lines = List.copyOf(lines);
  }
}
