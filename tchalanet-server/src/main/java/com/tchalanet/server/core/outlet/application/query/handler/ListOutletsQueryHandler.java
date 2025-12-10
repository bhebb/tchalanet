package com.tchalanet.server.core.outlet.application.query.handler;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.outlet.application.query.model.ListOutletsQuery;
import com.tchalanet.server.core.outlet.application.port.out.OutletRepositoryPort;
import com.tchalanet.server.core.outlet.domain.model.Outlet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class ListOutletsQueryHandler implements QueryHandler<ListOutletsQuery, List<Outlet>> {

  private final OutletRepositoryPort repository;

  @Override
  public List<Outlet> handle(ListOutletsQuery query) {
    return repository.findByTenantId(query.tenantId());
  }
}

