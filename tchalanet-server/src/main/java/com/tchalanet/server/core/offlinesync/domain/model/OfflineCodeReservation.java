package com.tchalanet.server.core.offlinesync.domain.model;

import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.OfflineCodeReservationId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.util.Objects;

public record OfflineCodeReservation(
    OfflineCodeReservationId id,
    TenantId tenantId,
    OfflineCodeBatchId codeBatchId,
    String offlineCode,
    OfflineCodeReservationStatus status,
    Instant reservedAt,
    Instant consumedAt
) {
  public OfflineCodeReservation {
    Objects.requireNonNull(id, "id is required");
    Objects.requireNonNull(tenantId, "tenantId is required");
    Objects.requireNonNull(codeBatchId, "codeBatchId is required");
    Objects.requireNonNull(status, "status is required");
    Objects.requireNonNull(reservedAt, "reservedAt is required");
    if (offlineCode == null || offlineCode.isBlank()) {
      throw new IllegalArgumentException("offlineCode is required");
    }
    if (consumedAt != null && consumedAt.isBefore(reservedAt)) {
      throw new IllegalArgumentException("consumedAt cannot be before reservedAt");
    }
  }
}

