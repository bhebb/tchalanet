package com.tchalanet.server.core.sales.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sales.application.port.out.TicketSettlementQueryPort;
import com.tchalanet.server.core.sales.infra.persistence.repository.TicketSettlementQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketSettlementQueryJpaAdapter implements TicketSettlementQueryPort {

  private final TicketSettlementQueryRepository repo;

  @Override
  public boolean existsPendingByDrawId(TenantId tenantId, DrawId drawId) {
    return repo.existsPending(tenantId.uuid(), drawId.uuid());
  }

  @Override
  public long countPendingByDrawId(TenantId tenantId, DrawId drawId) {
    return repo.countPending(tenantId.uuid(), drawId.uuid());
  }
}
