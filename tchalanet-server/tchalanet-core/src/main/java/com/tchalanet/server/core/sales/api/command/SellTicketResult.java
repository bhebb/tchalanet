package com.tchalanet.server.core.sales.api.command;

import com.tchalanet.server.common.types.id.ApprovalRequestId;

public record SellTicketResult(
    SoldTicketView ticket, SellTicketOutcome outcome, ApprovalRequestId approvalRequestId) {}
