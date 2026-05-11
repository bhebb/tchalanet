package com.tchalanet.server.core.payout.application.query.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.payout.domain.model.PayoutStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PayoutRow(
    PayoutId id,
    TicketId ticketId,
    BigDecimal amount,
    PayoutStatus status,
    Instant requestedAt,
    OutletId outletId,
    String outletName
) {}
