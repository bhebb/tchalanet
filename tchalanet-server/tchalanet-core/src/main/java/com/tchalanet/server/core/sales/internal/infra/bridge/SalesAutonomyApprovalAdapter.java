package com.tchalanet.server.core.sales.internal.infra.bridge;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.sales.internal.application.port.out.AutonomyApprovalPort;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class SalesAutonomyApprovalAdapter implements AutonomyApprovalPort {

  @SuppressWarnings("unused")
  private final QueryBus queryBus;

  public SalesAutonomyApprovalAdapter(QueryBus queryBus) {
    this.queryBus = queryBus;
  }

  @Override
  public AutonomyDecision evaluate(OutletId outletId, TerminalId terminalId, UserId sellerUserId, BigDecimal totalAmount) {
    return new AutonomyDecision(false, null);
  }
}

