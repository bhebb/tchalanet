package com.tchalanet.server.core.sales.internal.infra.bridge;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.sales.application.port.out.DrawLookupPort;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class SalesDrawLookupAdapter implements DrawLookupPort {

  @SuppressWarnings("unused")
  private final QueryBus queryBus;

  public SalesDrawLookupAdapter(QueryBus queryBus) {
    this.queryBus = queryBus;
  }

  @Override
  public Optional<DrawSnapshot> findById(DrawId drawId) {
    return Optional.empty();
  }
}

