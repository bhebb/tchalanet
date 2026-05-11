package com.tchalanet.server.core.offlinesync.domain.model;

import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import java.time.Instant;
import java.util.Objects;

public record OfflineCodeBatch(
    OfflineCodeBatchId id,
    TenantId tenantId,
    TerminalId terminalId,
    int allocatedCount,
    Instant issuedAt,
    Instant expiresAt
) {
  public OfflineCodeBatch {
    Objects.requireNonNull(id, "id is required");
    Objects.requireNonNull(tenantId, "tenantId is required");
    Objects.requireNonNull(terminalId, "terminalId is required");
    Objects.requireNonNull(issuedAt, "issuedAt is required");
    Objects.requireNonNull(expiresAt, "expiresAt is required");
    if (allocatedCount <= 0) {
      throw new IllegalArgumentException("allocatedCount must be > 0");
    }
    if (expiresAt.isBefore(issuedAt)) {
      throw new IllegalArgumentException("expiresAt must be after issuedAt");
    }
  }
}

