package com.tchalanet.server.core.sales.api.query.reconciliation;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import java.math.BigDecimal;

public record ExpectedTicketOutcomeRow(
    TicketId ticketId,
    String ticketCode,
    String publicCode,
    String displayCode,
    DrawId drawId,
    DrawResultId drawResultId,
    boolean shouldWin,
    TicketResultStatus expectedResultStatus,
    TicketSettlementStatus expectedSettlementStatus,
    BigDecimal expectedPayoutAmount,
    int winningLineCount
) {}
