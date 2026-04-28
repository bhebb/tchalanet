package com.tchalanet.server.core.session.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.session.application.port.out.SalesSessionTotalsAggregatePort;
import com.tchalanet.server.core.session.infra.persistence.repository.PayoutAggregateRepository;
import com.tchalanet.server.core.session.infra.persistence.repository.SalesAggregateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SalesSessionTotalsAggregateAdapter implements SalesSessionTotalsAggregatePort {

  private final SalesAggregateRepository salesAggRepo;
  private final PayoutAggregateRepository payoutAggRepo;

  @Override
  public TotalsAggregate compute(TenantId tenantId, SessionId sessionId) {
    var sales = salesAggRepo.computeTicketAgg(tenantId.uuid(), sessionId.uuid());
    var payout = payoutAggRepo.computePayoutAgg(tenantId.uuid(), sessionId.uuid());
    return new TotalsAggregate(sales.totalTickets(), sales.totalStake(), payout);
  }
}
