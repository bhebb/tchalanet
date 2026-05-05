package com.tchalanet.server.features.ticketverify.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Public ticket verification response. Must never expose internal IDs
 * (ticketId, drawId, tenantId, terminalId, addressId, etc.).
 */
public record TicketVerifyResponse(
    TicketVerifyStatus status,
    String publicCode,
    TicketVerifyPayoutStatus payoutStatus,
    BigDecimal totalAmount,
    BigDecimal winningAmount,
    Instant soldAt,
    TicketVerifyOutletView outlet,
    List<TicketVerifyLineItem> lines
) {}
