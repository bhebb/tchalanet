package com.tchalanet.server.core.payout.api.query.reconciliation;

import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.money.Money;
import java.time.Instant;

public record PayoutPaymentForDrawRow(
    PayoutId payoutId,
    TicketId ticketId,
    String ticketCode,
    String publicCode,
    String displayCode,
    Money amount,
    Instant paidAt
) {}
