package com.tchalanet.server.core.session.infra.persistence.adapter;

import com.tchalanet.server.core.session.application.port.out.PosSessionTotalsAggregatePort;
import com.tchalanet.server.core.session.infra.persistence.repository.PayoutAggregateRepository;
import com.tchalanet.server.core.session.infra.persistence.repository.SalesAggregateRepository;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PosSessionTotalsAggregateAdapter implements PosSessionTotalsAggregatePort {

  private final SalesAggregateRepository salesAggRepo;
  private final PayoutAggregateRepository payoutAggRepo;

  @Override
  public TotalsAggregate compute(UUID tenantId, UUID sessionId) {
    var sales = salesAggRepo.computeTicketAgg(tenantId, sessionId);
    var payout = payoutAggRepo.computePayoutAgg(tenantId, sessionId);
    return new TotalsAggregate(sales.totalTickets(), sales.totalStake(), payout);
  }
}
