package com.tchalanet.server.core.sales.internal.application.port.out;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** DTO used by printing adapters/handlers to represent a ticket for printing. */
public record TicketPrintView(
    UUID ticketId,
    String ticketCode,
    String publicCode,
    UUID terminalId,
    UUID drawId,
    Instant createdAt,
    BigDecimal totalAmount,
    String outletName,
    String channelCode,
    String drawChannelLabel,
    String drawWhenLabel,
    List<TicketPrintLine> lines
) {}
