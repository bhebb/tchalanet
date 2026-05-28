package com.tchalanet.server.core.payout.api.query;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.payout.internal.domain.model.PayoutClaimStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PayoutRow(
    PayoutId id,
    TicketId ticketId,
    BigDecimal amount,
    PayoutClaimStatus status,
    Instant openedAt,
    OutletId outletId,
    String outletName
) {}
