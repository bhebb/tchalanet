package com.tchalanet.server.core.payout.api.query;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.payout.domain.model.PayoutStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PayoutDetails(
    PayoutId id,
    TicketId ticketId,
    BigDecimal amount,
    PayoutStatus status,

    OutletId outletId,
    String outletName,

    SalesSessionId sessionId,
    TerminalId terminalId,

    UserId requestedBy,
    UserId approvedBy,
    UserId executedBy,

    Instant requestedAt,
    Instant approvedAt,
    Instant executedAt,

    String rejectionReason,
    String notes
) {
}
