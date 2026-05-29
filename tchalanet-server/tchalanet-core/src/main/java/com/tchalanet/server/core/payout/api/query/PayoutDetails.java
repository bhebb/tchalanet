package com.tchalanet.server.core.payout.api.query;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.payout.internal.domain.model.PayoutClaimSource;
import com.tchalanet.server.core.payout.internal.domain.model.PayoutClaimStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PayoutDetails(
    PayoutId id,
    TicketId ticketId,
    DrawId drawId,
    BigDecimal amount,
    PayoutClaimStatus status,
    PayoutClaimSource source,

    OutletId outletId,
    String outletName,

    SalesSessionId sessionId,
    TerminalId terminalId,

    UserId paidBy,
    UserId blockedBy,
    UserId cancelledBy,
    UserId reversedBy,

    Instant openedAt,
    Instant paidAt,
    Instant blockedAt,
    Instant cancelledAt,
    Instant reversedAt,

    String blockReason,
    String cancelReason,
    String reverseReason
) {}
