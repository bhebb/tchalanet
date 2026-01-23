package com.tchalanet.server.core.sales.infra.web.model;

import com.tchalanet.server.common.types.enums.TicketResultStatus;
import com.tchalanet.server.common.types.enums.TicketSaleStatus;
import com.tchalanet.server.common.types.enums.TicketSettlementStatus;

/**
 * Composite ticket status containing all three status dimensions.
 * This provides a complete view of the ticket's state across sale, result, and settlement.
 */
public record TicketStatus(
    TicketSaleStatus saleStatus,
    TicketResultStatus resultStatus,
    TicketSettlementStatus settlementStatus
) {}
