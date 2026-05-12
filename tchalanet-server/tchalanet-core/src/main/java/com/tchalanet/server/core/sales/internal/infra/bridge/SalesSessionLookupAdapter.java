package com.tchalanet.server.core.sales.internal.infra.bridge;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.sales.application.port.out.SalesSessionLookupPort;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class SalesSessionLookupAdapter implements SalesSessionLookupPort {

  @SuppressWarnings("unused")
  private final QueryBus queryBus;

  public SalesSessionLookupAdapter(QueryBus queryBus) {
    this.queryBus = queryBus;
  }

  @Override
  public Optional<SalesSessionSnapshot> findById(SalesSessionId salesSessionId) {
    return Optional.empty();
  }

  @Override
  public Optional<SalesSessionSnapshot> findOpenByTerminal(TerminalId terminalId, UserId sellerUserId) {
    return Optional.empty();
  }
}

