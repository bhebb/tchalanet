package com.tchalanet.server.core.payout.api.query.reconciliation;

import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.payout.internal.domain.model.PayoutClaimStatus;
import java.time.Instant;

public record PayoutClaimForDrawRow(
    PayoutId payoutId,
    TicketId ticketId,
    String ticketCode,
    String publicCode,
    String displayCode,
    PayoutClaimStatus status,
    Money amount,
    Instant openedAt,
    Instant paidAt,
    Instant cancelledAt
) {}
