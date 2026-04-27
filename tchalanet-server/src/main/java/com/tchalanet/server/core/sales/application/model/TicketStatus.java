package com.tchalanet.server.core.sales.application.model;

import com.tchalanet.server.common.types.enums.TicketResultStatus;
import com.tchalanet.server.common.types.enums.TicketSaleStatus;
import com.tchalanet.server.common.types.enums.TicketSettlementStatus;

/**
 * Shared POJO for ticket status (sale/result/settlement) usable by commands and queries.
 */
public record TicketStatus(
    TicketSaleStatus saleStatus,
    TicketResultStatus resultStatus,
    TicketSettlementStatus settlementStatus) {}

