package com.tchalanet.server.core.offlinesync.internal.domain.model;

import com.tchalanet.server.common.types.id.*;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

public record OfflineSaleSubmission(
    OfflineSaleSubmissionId id,
    TenantId tenantId,
    OfflineBatchId batchId,
    OfflineSalesGrantId grantId,
    OfflineCodeBatchId codeBatchId,
    String offlineCode,
    TerminalId terminalId,
    OutletId outletId,
    UserId sellerUserId,
    SalesSessionId salesSessionId,
    String clientTicketId,
    long localSequence,
    Instant createdAtDevice,
    Instant receivedAt,
    String payloadJson,
    String payloadHash,
    String signature,
    OfflineSubmissionStatus status,
    OfflineTechnicalRejectReason technicalRejectReason,
    SalesOfflineDecision salesDecision,
    SalesOfflineRejectReason salesRejectReason,
    Set<OfflineRiskFlag> riskFlags,
    TicketId salesTicketId
) {
  public OfflineSaleSubmission {
    Objects.requireNonNull(id, "id is required");
    Objects.requireNonNull(tenantId, "tenantId is required");
    Objects.requireNonNull(batchId, "batchId is required");
    Objects.requireNonNull(grantId, "grantId is required");
    Objects.requireNonNull(codeBatchId, "codeBatchId is required");
    Objects.requireNonNull(terminalId, "id is required");
    Objects.requireNonNull(outletId, "outletId is required");
    Objects.requireNonNull(sellerUserId, "sellerUserId is required");
    Objects.requireNonNull(salesSessionId, "salesSessionId is required");
    Objects.requireNonNull(createdAtDevice, "createdAtDevice is required");
    Objects.requireNonNull(receivedAt, "receivedAt is required");
    Objects.requireNonNull(status, "status is required");
    if (offlineCode == null || offlineCode.isBlank()) throw new IllegalArgumentException("offlineCode is required");
    if (clientTicketId == null || clientTicketId.isBlank()) throw new IllegalArgumentException("clientTicketId is required");
    if (payloadJson == null || payloadJson.isBlank()) throw new IllegalArgumentException("payloadJson is required");
    if (payloadHash == null || payloadHash.isBlank()) throw new IllegalArgumentException("payloadHash is required");
    if (signature == null || signature.isBlank()) throw new IllegalArgumentException("signature is required");
  }
}
