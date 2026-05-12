package com.tchalanet.server.features.ticketverify.model;

import java.math.BigDecimal;

public record TicketVerifyLineItem(
    String gameCode,
    String betType,
    String selection,
    BigDecimal stake,
    BigDecimal potentialPayout
) {}
