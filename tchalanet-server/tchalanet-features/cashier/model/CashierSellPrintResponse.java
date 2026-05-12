package com.tchalanet.server.features.cashier.model;

import com.tchalanet.server.common.types.id.ApprovalRequestId;
import com.tchalanet.server.core.sales.application.command.model.SellTicketOutcome;

public record CashierSellPrintResponse(
    CashierTicketView ticket,
    SellTicketOutcome outcome,
    ApprovalRequestId approvalRequestId,
    CashierPrintableReceipt receipt
) {}
