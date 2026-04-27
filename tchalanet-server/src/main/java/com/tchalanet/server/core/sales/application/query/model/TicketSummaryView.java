package com.tchalanet.server.core.sales.application.query.model;

import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.application.model.TicketStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Lightweight view for ticket summaries (used in lists).
 */
public record TicketSummaryView(
    TicketId id,
    String ticketCode,
    String publicCode,
    TicketStatus status,
    BigDecimal totalAmount,
    Instant createdAt,
    String terminalLabel,
    String drawInfo
) {
}
