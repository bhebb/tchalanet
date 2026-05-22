package com.tchalanet.server.core.sales.api.command.cancel;

import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.model.sale.SaleIssueView;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record CancelTicketResult(
    TicketId ticketId,
    CancelTicketOutcome outcome,
    Instant cancelledAt,
    List<SaleIssueView> issues
) {
    public CancelTicketResult {
        Objects.requireNonNull(ticketId, "ticketId is required");
        Objects.requireNonNull(outcome, "outcome is required");
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public enum CancelTicketOutcome {
        CANCELLED,
        ALREADY_CANCELLED,
        REJECTED
    }
}
