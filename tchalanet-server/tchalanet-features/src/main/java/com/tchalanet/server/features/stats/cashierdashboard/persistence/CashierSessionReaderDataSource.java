package com.tchalanet.server.features.stats.cashierdashboard.persistence;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.session.api.query.GetOpenedSalesSessionQuery;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CashierSessionReaderDataSource implements CashierSessionReader {

  private final QueryBus queryBus;

  @Override
  public boolean hasOpenSession(UUID tenantId, UUID cashierId) {
    var open =
        queryBus.ask(
            new GetOpenedSalesSessionQuery(TenantId.of(tenantId), null, UserId.of(cashierId), null));
    return open != null && !open.isEmpty();
  }
}
