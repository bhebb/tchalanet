package com.tchalanet.server.core.offlinesync.internal.domain.model;

import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.OfflineSalesGrantId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;
import java.util.Objects;

public record OfflineSalesGrant(
    OfflineSalesGrantId id,
    TenantId tenantId,
    TerminalId terminalId,
    OutletId outletId,
    SalesSessionId salesSessionId,
    UserId sellerUserId,
    OfflineCodeBatchId codeBatchId,
    OfflineSalesGrantStatus status,
    Instant issuedAt,
    Instant expiresAt
) {
  public OfflineSalesGrant {
    Objects.requireNonNull(id, "id is required");
    Objects.requireNonNull(tenantId, "tenantId is required");
    Objects.requireNonNull(terminalId, "terminalId is required");
    Objects.requireNonNull(outletId, "outletId is required");
    Objects.requireNonNull(salesSessionId, "salesSessionId is required");
    Objects.requireNonNull(sellerUserId, "sellerUserId is required");
    Objects.requireNonNull(codeBatchId, "codeBatchId is required");
    Objects.requireNonNull(status, "status is required");
    Objects.requireNonNull(issuedAt, "issuedAt is required");
    Objects.requireNonNull(expiresAt, "expiresAt is required");
    if (expiresAt.isBefore(issuedAt)) {
      throw new IllegalArgumentException("expiresAt must be after issuedAt");
    }
  }
}

