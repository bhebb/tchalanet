package com.tchalanet.server.core.session.api.query;

import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.session.internal.domain.model.SalesSessionStatus;
import java.time.Instant;

public record ValidatedSalesSessionOperationView(
    SalesSessionId salesSessionId,
    SalesSessionStatus status,
    Instant openedAt,
    Instant closedAt,
    Instant finalizedAt,
    UserId finalizedBy
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
