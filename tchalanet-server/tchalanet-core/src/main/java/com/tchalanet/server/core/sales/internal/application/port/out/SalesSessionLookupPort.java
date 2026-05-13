package com.tchalanet.server.core.sales.internal.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SalesSessionLookupPort {
  Optional<SalesSessionSnapshot> findById(SalesSessionId salesSessionId);

  default Optional<SalesSessionSnapshot> findOpenByTerminal(TerminalId terminalId, UserId sellerUserId) {
    return Optional.empty();
  }

  default List<SalesSessionId> findSessionIds(OutletId outletId, Instant from, Instant to) {
    return List.of();
  }

  record SalesSessionSnapshot(
      SalesSessionId sessionId,
      OutletId outletId,
      TerminalId terminalId,
      UserId sellerUserId,
      Instant openedAt,
      Instant closedAt,
      boolean finalized
  ) {}
}
