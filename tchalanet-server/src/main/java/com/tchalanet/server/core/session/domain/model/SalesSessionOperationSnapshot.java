package com.tchalanet.server.core.session.domain.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;

public record SalesSessionOperationSnapshot(
    TenantId tenantId,
    SalesSessionId salesSessionId,
    TerminalId terminalId,
    OutletId outletId,
    UserId sellerUserId,
    SalesSessionStatus status,
    Instant openedAt,
    Instant closedAt,
    Instant finalizedAt,
    UserId finalizedBy,
    String finalizeReason
) {
  public boolean open() {
    return status == SalesSessionStatus.OPEN;
  }

  public boolean closed() {
    return status == SalesSessionStatus.CLOSED;
  }

  public boolean cancelled() {
    return status == SalesSessionStatus.CANCELLED;
  }

  public boolean finalized() {
    return status == SalesSessionStatus.FINALIZED;
  }
}
