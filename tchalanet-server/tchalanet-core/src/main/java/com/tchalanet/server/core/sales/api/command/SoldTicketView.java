package com.tchalanet.server.core.sales.api.command;

import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.model.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.TicketSettlementStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record SoldTicketView(
    TicketId ticketId,
    String ticketCode,
    String publicCode,
    TicketSaleStatus saleStatus,
    TicketResultStatus resultStatus,
    TicketSettlementStatus settlementStatus,
    BigDecimal totalAmount,
    Instant createdAt) {}
