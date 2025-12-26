package com.tchalanet.server.core.sales.infra.bridge;

import com.tchalanet.server.common.types.enums.TicketStatus;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.outlet.application.port.out.SalesTicketAdminPort;
import com.tchalanet.server.core.outlet.application.port.out.SessionLookupPort;
import com.tchalanet.server.core.sales.infra.persistence.repository.SpringTicketJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SalesTicketAdminAdapter implements SalesTicketAdminPort {

  private final SpringTicketJpaRepository repo;
  private final SessionLookupPort sessionLookup;

  @Override
  public TicketCloseStats getCloseStats(
      TenantId tenantId, OutletId outletId, Instant from, Instant to) {
    List<SessionId> sessions = sessionLookup.findSessionIds(tenantId, outletId, from, to);
    List<java.util.UUID> sessionUuids =
        sessions.stream().map(SessionId::uuid).collect(Collectors.toList());

    long total =
        repo.countByTenantIdAndSessionIdInAndCreatedAtBetween(
            tenantId.uuid(), sessionUuids, from, to);
    long sold =
        repo.countByTenantIdAndSessionIdInAndCreatedAtBetweenAndStatus(
            tenantId.uuid(), sessionUuids, from, to, TicketStatus.SOLD);
    long voided =
        repo.countByTenantIdAndSessionIdInAndCreatedAtBetweenAndStatus(
            tenantId.uuid(), sessionUuids, from, to, TicketStatus.VOIDED);
    long resultedWin =
        repo.countByTenantIdAndSessionIdInAndCreatedAtBetweenAndStatus(
            tenantId.uuid(), sessionUuids, from, to, TicketStatus.RESULTED_WIN);
    long resultedLoss =
        repo.countByTenantIdAndSessionIdInAndCreatedAtBetweenAndStatus(
            tenantId.uuid(), sessionUuids, from, to, TicketStatus.RESULTED_LOSS);
    long paid =
        repo.countByTenantIdAndSessionIdInAndCreatedAtBetweenAndStatus(
            tenantId.uuid(), sessionUuids, from, to, TicketStatus.PAID);

    return new TicketCloseStats(total, sold, voided, resultedWin, resultedLoss, paid);
  }

  @Override
  public void refuseNewTickets(TenantId tenantId, OutletId outletId) {
    // TODO: implement gating to prevent new sales; for V1 no-op
  }

  @Override
  public void allowNewTickets(TenantId tenantId, OutletId outletId) {
    // TODO: re-enable ticket creation
  }
}
