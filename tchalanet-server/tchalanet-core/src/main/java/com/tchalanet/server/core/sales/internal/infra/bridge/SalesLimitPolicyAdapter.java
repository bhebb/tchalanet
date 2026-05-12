package com.tchalanet.server.core.sales.internal.infra.bridge;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.sales.application.port.out.LimitPolicyPort;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class SalesLimitPolicyAdapter implements LimitPolicyPort {

  @SuppressWarnings("unused")
  private final QueryBus queryBus;

  public SalesLimitPolicyAdapter(QueryBus queryBus) {
    this.queryBus = queryBus;
  }

  @Override
  public LimitDecision evaluateSale(OutletId outletId, TerminalId terminalId, UserId sellerUserId, BigDecimal stakeAmount, BigDecimal totalAmount) {
    return new LimitDecision(false, false, null);
  }
}

