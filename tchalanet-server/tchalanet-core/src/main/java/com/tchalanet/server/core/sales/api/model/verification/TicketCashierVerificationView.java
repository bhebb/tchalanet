package com.tchalanet.server.core.sales.api.model.verification;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import java.time.Instant;

public record TicketCashierVerificationView(
    TicketId ticketId,
    String ticketCode,
    String publicCode,
    String displayCode,
    CustomerTicketStatus customerStatus,
    TicketSaleStatus saleStatus,
    TicketResultStatus resultStatus,
    TicketSettlementStatus settlementStatus,
    Money totalAmount,
    Money winningAmount,
    Instant placedAt,
    DrawId drawId,
    Instant drawScheduledAt
) {}
