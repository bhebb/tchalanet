package com.tchalanet.server.core.sales.api.command;

import com.tchalanet.server.common.types.id.ApprovalRequestId;
import com.tchalanet.server.core.sales.api.command.SellTicketOutcome;
import com.tchalanet.server.core.sales.internal.domain.model.Ticket;

public record SellTicketResult(Ticket ticket, SellTicketOutcome outcome, ApprovalRequestId approvalRequestId) {}

