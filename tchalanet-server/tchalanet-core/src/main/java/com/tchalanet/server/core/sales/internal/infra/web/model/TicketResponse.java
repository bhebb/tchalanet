package com.tchalanet.server.core.sales.internal.infra.web.model;

import java.math.BigDecimal;

public record TicketResponse(
    String ticketId,
    String publicCode,
    String saleStatus,
    String saleOrigin,
    String syncStatus,
    BigDecimal stakeAmount,
    BigDecimal feeAmount,
    BigDecimal totalAmount
) {}
