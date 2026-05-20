package com.tchalanet.server.core.sales.api.command.sell;

import com.tchalanet.server.common.types.id.ApprovalRequestId;
import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.Ticket;

import java.util.List;
import java.util.Objects;

public record SellTicketResult(
    Ticket ticket,
    SellTicketOutcome outcome,
    ApprovalRequestId approvalRequestId,
    List<ApiNotice> notices
) {
    public SellTicketResult {
        Objects.requireNonNull(ticket);
        Objects.requireNonNull(outcome);
        Objects.requireNonNull(notices);
        notices = List.copyOf(notices);
        if (outcome == SellTicketOutcome.PENDING_APPROVAL && approvalRequestId == null) {
            throw new IllegalArgumentException(
                "approvalRequestId is required when outcome is PENDING_APPROVAL");
        }
    }
}
