package com.tchalanet.server.core.sales.api.command.payout;

import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;

public record MarkTicketPayoutReversedResult(
    TicketId ticketId,
    TicketSettlementStatus settlementStatus
) {}
