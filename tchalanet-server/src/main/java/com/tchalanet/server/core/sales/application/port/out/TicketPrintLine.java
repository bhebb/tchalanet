package com.tchalanet.server.core.sales.application.port.out;

import java.math.BigDecimal;

/** DTO representing a single printable line on a ticket. */
public record TicketPrintLine(
    String gameCode, String selection, BigDecimal stake, BigDecimal potentialPayout) {}
