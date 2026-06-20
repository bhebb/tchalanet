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
 * <p>Covers both:
 * <ul>
 *   <li>direct {@code APPROVED} placement (the common path),</li>
 *   <li>{@code PENDING_APPROVAL} placement (limit triggered, autonomy != NONE).</li>
 * </ul>
 *
 * <p>Listeners that aggregate <strong>official</strong> sales must filter on
 * {@code saleStatus == APPROVED} and also subscribe to
 * {@link TicketApprovedEvent} for the PENDING → APPROVED transition. Both
 * paths together cover the lifecycle to APPROVED, and listener implementations
 * must be idempotent (using {@code ticketId} as dedup key).
 *
 * <p>Payload is grouped by concern (context / money / lines / offline) using
 * sibling payload records under {@code .payload}. This keeps the event API
 * stable as new concerns are added (extend the relevant payload record rather
 * than the event signature).
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
    TicketSaleStatus saleStatus,           // APPROVED or PENDING_APPROVAL
    TicketSaleChannel saleChannel,

    // Grouped payloads
    TicketContextPayload context,
    TicketMoneyPayload money,
    List<TicketLinePlacedItem> lines,
    PromotionDecision promotionDecision
) implements DomainEvent {
    public static final int CURRENT_SCHEMA = 2;

    public TicketPlacedEvent {
        if (saleStatus != TicketSaleStatus.APPROVED
            && saleStatus != TicketSaleStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException(
                "TicketPlacedEvent must carry APPROVED or PENDING_APPROVAL, got " + saleStatus);
        }
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("lines must not be empty");
        }
        lines = List.copyOf(lines);
    }
}
