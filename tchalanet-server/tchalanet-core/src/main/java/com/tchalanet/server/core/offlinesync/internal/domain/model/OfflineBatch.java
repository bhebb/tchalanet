package com.tchalanet.server.core.offlinesync.internal.domain.model;

import com.tchalanet.server.common.types.id.OfflineBatchId;
import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.OfflineSalesGrantId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import java.time.Instant;
import java.util.Objects;

public record OfflineBatch(
    OfflineBatchId id,
    TenantId tenantId,
    TerminalId terminalId,
    OfflineSalesGrantId grantId,
    OfflineCodeBatchId codeBatchId,
    String clientBatchId,
    Instant receivedAt,
    OfflineBatchStatus status,
    int ticketCount,
    int technicalRejectCount,
    int salesAcceptCount,
    int salesRejectCount,
    int reviewCount
) {
  public OfflineBatch {
    Objects.requireNonNull(id, "id is required");
    Objects.requireNonNull(tenantId, "tenantId is required");
    Objects.requireNonNull(terminalId, "terminalId is required");
    Objects.requireNonNull(grantId, "grantId is required");
    Objects.requireNonNull(codeBatchId, "codeBatchId is required");
    Objects.requireNonNull(receivedAt, "receivedAt is required");
    Objects.requireNonNull(status, "status is required");
    if (clientBatchId == null || clientBatchId.isBlank()) {
      throw new IllegalArgumentException("clientBatchId is required");
    }
    if (ticketCount < 0 || technicalRejectCount < 0 || salesAcceptCount < 0 || salesRejectCount < 0 || reviewCount < 0) {
      throw new IllegalArgumentException("batch counters must be >= 0");
    }
  }
}

