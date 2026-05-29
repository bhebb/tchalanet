package com.tchalanet.server.core.sales.api.query.reconciliation;

import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import java.time.Instant;

public record ActualTicketStateRow(
    TicketId ticketId,
    String ticketCode,
    String publicCode,
    String displayCode,
    TicketSaleStatus saleStatus,
    TicketResultStatus resultStatus,
    TicketSettlementStatus settlementStatus,
    Money actualPotentialPayout,
    Instant placedAt,
    boolean cancelled,
    boolean voided
) {}
