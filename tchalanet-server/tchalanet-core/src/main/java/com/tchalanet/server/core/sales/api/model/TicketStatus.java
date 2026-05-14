package com.tchalanet.server.core.sales.api.model;

import com.tchalanet.server.core.sales.api.model.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.TicketSettlementStatus;

/**
 * Shared POJO for ticket status (sale/result/settlement) usable by commands and queries.
 */
public record TicketStatus(
    TicketSaleStatus saleStatus,
    TicketResultStatus resultStatus,
    TicketSettlementStatus settlementStatus) {}

