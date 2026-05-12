package com.tchalanet.server.core.sales.internal.infra.bridge;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.sales.internal.application.port.out.TerminalSalesContextPort;
import org.springframework.stereotype.Component;

@Component
public class SalesTerminalContextAdapter implements TerminalSalesContextPort {

  @SuppressWarnings("unused")
  private final QueryBus queryBus;

  public SalesTerminalContextAdapter(QueryBus queryBus) {
    this.queryBus = queryBus;
  }

  @Override
  public TerminalSalesContext getContext(TerminalId terminalId, UserId sellerUserId) {
    return new TerminalSalesContext(terminalId, OutletId.nullableOf(null), false, false, false);
  }
}

