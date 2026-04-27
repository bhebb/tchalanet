package com.tchalanet.server.core.sales.infra.bridge;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.core.outlet.application.port.out.SalesTicketAdminPort;
import com.tchalanet.server.core.outlet.application.port.out.SessionLookupPort;
import com.tchalanet.server.core.sales.infra.persistence.repository.SpringTicketJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SalesTicketAdminAdapter implements SalesTicketAdminPort {

  private final SpringTicketJpaRepository repo;
  private final SessionLookupPort sessionLookup;

  @Override
  public TicketCloseStats getCloseStats(
      OutletId outletId, Instant from, Instant to) {
    List<SessionId> sessions = sessionLookup.findSessionIds(outletId, from, to);
    List<java.util.UUID> sessionUuids =
        sessions.stream().map(SessionId::value).collect(Collectors.toList());

    long total = repo.countBySessionIdInAndCreatedAtBetween(sessionUuids, from, to);
    long sold = repo.countBySessionIdInAndCreatedAtBetweenAndSaleStatus(sessionUuids, from, to, com.tchalanet.server.common.types.enums.TicketSaleStatus.SOLD);
    long voided = repo.countBySessionIdInAndCreatedAtBetweenAndSaleStatus(sessionUuids, from, to, com.tchalanet.server.common.types.enums.TicketSaleStatus.VOID);
    long resultedWin = repo.countBySessionIdInAndCreatedAtBetweenAndResultStatus(sessionUuids, from, to, com.tchalanet.server.common.types.enums.TicketResultStatus.WON);
    long resultedLoss = repo.countBySessionIdInAndCreatedAtBetweenAndResultStatus(sessionUuids, from, to, com.tchalanet.server.common.types.enums.TicketResultStatus.LOST);
    long paid = repo.countBySessionIdInAndCreatedAtBetweenAndSettlementStatus(sessionUuids, from, to, com.tchalanet.server.common.types.enums.TicketSettlementStatus.SETTLED);

    return new TicketCloseStats(total, sold, voided, resultedWin, resultedLoss, paid);
  }

  @Override
  public void refuseNewTickets(OutletId outletId) {
    // no-op v1
  }

  @Override
  public void allowNewTickets(OutletId outletId) {
    // no-op v1
  }
}
