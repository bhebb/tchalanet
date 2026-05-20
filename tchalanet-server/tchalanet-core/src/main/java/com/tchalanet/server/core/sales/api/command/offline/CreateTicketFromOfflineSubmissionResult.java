package com.tchalanet.server.core.sales.api.command.offline;

import com.tchalanet.server.common.types.id.TicketId;

public record CreateTicketFromOfflineSubmissionResult(
    Outcome outcome,
    TicketId ticketId,
    String rejectionCode,
    String rejectionReason
) {
    public enum Outcome { PROMOTED, BUSINESS_REJECTED, DUPLICATE }

    public static CreateTicketFromOfflineSubmissionResult promoted(TicketId ticketId) {
        return new CreateTicketFromOfflineSubmissionResult(Outcome.PROMOTED, ticketId, null, null);
    }

    public static CreateTicketFromOfflineSubmissionResult duplicate(TicketId existingTicketId) {
        return new CreateTicketFromOfflineSubmissionResult(Outcome.DUPLICATE, existingTicketId, null, null);
    }

    public static CreateTicketFromOfflineSubmissionResult businessRejected(String code, String reason) {
        return new CreateTicketFromOfflineSubmissionResult(Outcome.BUSINESS_REJECTED, null, code, reason);
    }
}
