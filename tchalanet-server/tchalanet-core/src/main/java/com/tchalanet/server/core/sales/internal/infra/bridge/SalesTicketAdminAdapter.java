package com.tchalanet.server.core.sales.internal.infra.bridge;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.core.outlet.internal.application.port.out.SalesTicketAdminPort;
import com.tchalanet.server.core.sales.internal.application.port.out.SalesSessionLookupPort;
import com.tchalanet.server.core.sales.internal.infra.persistence.repository.SpringTicketJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SalesTicketAdminAdapter implements SalesTicketAdminPort {

  private final SpringTicketJpaRepository repo;
  private final SalesSessionLookupPort sessionLookup;

  @Override
  public TicketCloseStats getCloseStats(
      OutletId outletId, Instant from, Instant to) {
    List<SalesSessionId> sessions = sessionLookup.findSessionIds(outletId, from, to);
    if (sessions.isEmpty()) {
      return new TicketCloseStats(0, 0, 0, 0, 0, 0);
    }
    List<java.util.UUID> sessionUuids =
        sessions.stream().map(SalesSessionId::value).collect(Collectors.toList());

    long total = repo.countBySessionIdInAndDeletedAtIsNull(sessionUuids);
    long sold = repo.countBySessionIdInAndSaleStatusAndDeletedAtIsNull(sessionUuids, com.tchalanet.server.common.types.enums.TicketSaleStatus.SOLD);
    long voided = repo.countBySessionIdInAndSaleStatusAndDeletedAtIsNull(sessionUuids, com.tchalanet.server.common.types.enums.TicketSaleStatus.VOID);
    long resultedWin = repo.countBySessionIdInAndResultStatusAndDeletedAtIsNull(sessionUuids, com.tchalanet.server.common.types.enums.TicketResultStatus.WON);
    long resultedLoss = repo.countBySessionIdInAndResultStatusAndDeletedAtIsNull(sessionUuids, com.tchalanet.server.common.types.enums.TicketResultStatus.LOST);
    long paid = repo.countBySessionIdInAndSettlementStatusAndDeletedAtIsNull(sessionUuids, com.tchalanet.server.common.types.enums.TicketSettlementStatus.SETTLED);

    return new TicketCloseStats(total, sold, voided, resultedWin, resultedLoss, paid);
  }

  @Override
  public void refuseNewTickets(OutletId outletId) {
    throw new UnsupportedOperationException("refuseNewTickets is not implemented");
  }

  @Override
  public void allowNewTickets(OutletId outletId) {
    throw new UnsupportedOperationException("allowNewTickets is not implemented");
  }
}
