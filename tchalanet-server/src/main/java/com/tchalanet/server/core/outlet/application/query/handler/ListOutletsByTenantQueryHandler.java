package com.tchalanet.server.core.outlet.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.outlet.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.application.query.model.ListOutletsByTenantQuery;
import com.tchalanet.server.core.outlet.application.query.model.OutletView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@UseCase
@RequiredArgsConstructor
@Component
public class ListOutletsByTenantQueryHandler implements QueryHandler<ListOutletsByTenantQuery, List<OutletView>> {

  private final OutletReaderPort reader;

  @Override
  public List<OutletView> handle(ListOutletsByTenantQuery q) {
    var list = reader.listByTenant();
    return list.stream().map(o -> new OutletView(
        o.id(),
        o.tenantId(),
        o.name(),
        o.slug(),
        o.dayClosed(),
        o.receiptPrintingEnabled(),
        null
    )).collect(Collectors.toList());
  }
}
