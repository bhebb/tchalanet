package com.tchalanet.server.core.payout.infra.web.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.TicketId;

import java.math.BigDecimal;
import java.time.Instant;

public record PayoutRowResponse(
    PayoutId id,
    TicketId ticketId,
    BigDecimal amount,
    String status,
    Instant requestedAt,
    OutletId outletId,
    String outletName
) {}
