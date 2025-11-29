package com.tchalanet.server.core.sales.web.dto;

import com.tchalanet.server.core.sales.domain.model.TicketStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TicketSummaryResponse(
    UUID id,
    String ticketCode,
    String publicCode,
    TicketStatus status,
    BigDecimal totalAmount,
    Instant createdAt,
    String terminalLabel,
    String drawInfo) {}
