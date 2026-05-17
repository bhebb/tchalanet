package com.tchalanet.server.core.offlinesync.internal.domain.model;

import com.tchalanet.server.common.types.id.OfflineBatchId;
import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;
import com.tchalanet.server.common.types.id.OfflineSalesGrantId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.offlinesync.api.model.OfflineSubmissionStatus;
import java.time.Instant;

public record OfflineSubmission(
    OfflineSaleSubmissionId id,
    TenantId tenantId,
    OfflineBatchId batchId,
    OfflineSalesGrantId grantId,
    String clientSaleId,
    String payload,
    OfflineSubmissionStatus status,
    String techRejectionReason,
    String salesRejectionCode,
    TicketId ticketId,
    int attemptCount,
    Instant nextProcessingAt,
    Instant submittedAt,
    Instant processedAt
) {
    public OfflineSubmission withStatus(OfflineSubmissionStatus newStatus) {
        return new OfflineSubmission(id, tenantId, batchId, grantId, clientSaleId, payload,
            newStatus, techRejectionReason, salesRejectionCode, ticketId, attemptCount,
            nextProcessingAt, submittedAt, processedAt);
    }

    public OfflineSubmission markTechRejected(String reason) {
        return new OfflineSubmission(id, tenantId, batchId, grantId, clientSaleId, payload,
            OfflineSubmissionStatus.TECH_REJECTED, reason, salesRejectionCode, ticketId,
            attemptCount, nextProcessingAt, submittedAt, processedAt);
    }

    public OfflineSubmission markSalesAccepted(TicketId newTicketId, Instant now) {
        return new OfflineSubmission(id, tenantId, batchId, grantId, clientSaleId, payload,
            OfflineSubmissionStatus.SALES_ACCEPTED, techRejectionReason, salesRejectionCode,
            newTicketId, attemptCount, null, submittedAt, now);
    }

    public OfflineSubmission markSalesRejected(String code, Instant now) {
        return new OfflineSubmission(id, tenantId, batchId, grantId, clientSaleId, payload,
            OfflineSubmissionStatus.SALES_REJECTED, techRejectionReason, code,
            ticketId, attemptCount, null, submittedAt, now);
    }

    public OfflineSubmission incrementAttemptAndRetry(Instant nextAt) {
        return new OfflineSubmission(id, tenantId, batchId, grantId, clientSaleId, payload,
            OfflineSubmissionStatus.RETRY_PENDING, techRejectionReason, salesRejectionCode,
            ticketId, attemptCount + 1, nextAt, submittedAt, processedAt);
    }
}
