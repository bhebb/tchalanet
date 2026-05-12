package com.tchalanet.server.core.sales.internal.domain.model;

import com.tchalanet.server.common.types.id.OfflineBatchId;
import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;
import java.time.Instant;
import java.util.Objects;

public record OfflineSaleRef(
    OfflineSaleSubmissionId submissionId,
    OfflineBatchId batchId,
    OfflineCodeBatchId codeBatchId,
    String offlineCode,
    String clientTicketId,
    long localSequence,
    Instant createdAtDevice
) {
  public OfflineSaleRef {
    Objects.requireNonNull(submissionId, "submissionId is required");
    Objects.requireNonNull(batchId, "batchId is required");
    Objects.requireNonNull(codeBatchId, "codeBatchId is required");
    Objects.requireNonNull(createdAtDevice, "createdAtDevice is required");
    if (offlineCode == null || offlineCode.isBlank()) throw new IllegalArgumentException("offlineCode is required");
    if (clientTicketId == null || clientTicketId.isBlank()) throw new IllegalArgumentException("clientTicketId is required");
  }
}
