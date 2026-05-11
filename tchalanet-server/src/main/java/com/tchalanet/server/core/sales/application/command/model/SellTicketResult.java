package com.tchalanet.server.core.sales.application.command.model;

import com.tchalanet.server.common.types.id.ApprovalRequestId;
import com.tchalanet.server.core.sales.domain.model.Ticket;

public record SellTicketResult(
    Ticket ticket,
    SellTicketOutcome outcome,
    ApprovalRequestId approvalRequestId
) {}

