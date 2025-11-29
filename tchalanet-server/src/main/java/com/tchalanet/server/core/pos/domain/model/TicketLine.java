package com.tchalanet.server.core.pos.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record TicketLine(
    TicketLineId id,
    UUID ticketId,
    String gameCode,
    String selection,
    BigDecimal stake,
    BigDecimal oddsSnapshot,
    BigDecimal potentialPayout) {}
