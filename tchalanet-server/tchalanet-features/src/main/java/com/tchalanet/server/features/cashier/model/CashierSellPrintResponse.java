package com.tchalanet.server.features.cashier.model;

import com.tchalanet.server.common.types.id.ApprovalRequestId;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketOutcome;

public record CashierSellPrintResponse(
    CashierTicketView ticket,
    SellTicketOutcome outcome,
    ApprovalRequestId approvalRequestId,
    CashierPrintableReceipt receipt
) {}
