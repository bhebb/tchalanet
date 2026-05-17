package com.tchalanet.server.core.sales.api.command.offline;

import com.tchalanet.server.common.types.id.TicketId;

public record ProcessOfflineSubmissionForSalesResult(
    OfflineSalesDecisionStatus decision,
    TicketId ticketId,
    String rejectionCode
) {
    public static ProcessOfflineSubmissionForSalesResult accepted(TicketId ticketId) {
        return new ProcessOfflineSubmissionForSalesResult(
            OfflineSalesDecisionStatus.ACCEPTED, ticketId, null);
    }

    public static ProcessOfflineSubmissionForSalesResult rejected(String rejectionCode) {
        return new ProcessOfflineSubmissionForSalesResult(
            OfflineSalesDecisionStatus.REJECTED, null, rejectionCode);
    }

    public static ProcessOfflineSubmissionForSalesResult reviewRequired() {
        return new ProcessOfflineSubmissionForSalesResult(
            OfflineSalesDecisionStatus.REVIEW_REQUIRED, null, null);
    }
}
