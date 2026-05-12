package com.tchalanet.server.core.outlet.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.api.query.GetOutletSalesCapabilityQuery;
import com.tchalanet.server.core.outlet.internal.domain.model.SalesCapability;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetOutletSalesCapabilityQueryHandler
    implements QueryHandler<GetOutletSalesCapabilityQuery, SalesCapability> {

  private final OutletReaderPort reader;

  @Override
  public SalesCapability handle(GetOutletSalesCapabilityQuery query) {
    return reader.getRequired(query.outletId()).salesCapability();
  }
}
