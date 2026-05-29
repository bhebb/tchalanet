package com.tchalanet.server.core.sales.api.command.sell;

import com.tchalanet.server.common.types.id.ApprovalRequestId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.sale.SaleActionAvailability;
import com.tchalanet.server.core.sales.api.model.sale.SaleIssueView;
import com.tchalanet.server.core.sales.api.model.sale.TicketBackupInfo;

import java.util.List;

public record SellTicketResult(
    SoldTicketView ticket,
    SellTicketOutcome outcome,
    ApprovalRequestId approvalRequestId,
    List<ApiNotice> notices,
    List<SaleIssueView> issues,
    TicketBackupInfo backup,
    SaleActionAvailability actionAvailability,
    String sellerInstruction
) {
    public SellTicketResult {
        if (outcome == null) {
            throw new IllegalArgumentException("outcome is required");
        }
        if (outcome != SellTicketOutcome.REJECTED && ticket == null) {
            throw new IllegalArgumentException("ticket is required unless outcome is REJECTED");
        }
        notices = List.copyOf(notices);
        issues = issues == null ? List.of() : List.copyOf(issues);
        if (outcome == SellTicketOutcome.PENDING_APPROVAL && approvalRequestId == null) {
            throw new IllegalArgumentException(
                "approvalRequestId is required when outcome is PENDING_APPROVAL");
        }
        if (outcome == SellTicketOutcome.REJECTED && backup != null) {
            throw new IllegalArgumentException("backup must be null when outcome is REJECTED");
        }
        actionAvailability = actionAvailability == null
            ? defaultActions(outcome)
            : actionAvailability;
    }

    public SellTicketResult(
        SoldTicketView ticket,
        SellTicketOutcome outcome,
        ApprovalRequestId approvalRequestId,
        List<ApiNotice> notices
    ) {
        this(ticket, outcome, approvalRequestId, notices, List.of(), null, null, null);
    }

    private static SaleActionAvailability defaultActions(SellTicketOutcome outcome) {
        return outcome == SellTicketOutcome.REJECTED
            ? SaleActionAvailability.rejected()
            : SaleActionAvailability.accepted();
    }

    public TicketId ticketId() {
        return ticket == null ? null : ticket.ticketId();
    }

    public String ticketCode() {
        return ticket == null ? null : ticket.ticketCode();
    }

    public String publicCode() {
        return ticket == null ? null : ticket.publicCode();
    }

    public String displayCode() {
        return ticket == null ? null : ticket.displayCode();
    }

    public TicketSaleStatus saleStatus() {
        return ticket == null ? null : ticket.saleStatus();
    }
}
