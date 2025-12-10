package com.tchalanet.server.core.outlet.application.query.handler;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.outlet.application.query.model.GetOutletByIdQuery;
import com.tchalanet.server.core.outlet.application.port.out.OutletRepositoryPort;
import com.tchalanet.server.core.outlet.domain.model.Outlet;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class GetOutletByIdQueryHandler implements QueryHandler<GetOutletByIdQuery, Optional<Outlet>> {

  private final OutletRepositoryPort repository;

  @Override
  public Optional<Outlet> handle(GetOutletByIdQuery query) {
    return repository.findById(query.outletId(), query.tenantId());
  }
}

