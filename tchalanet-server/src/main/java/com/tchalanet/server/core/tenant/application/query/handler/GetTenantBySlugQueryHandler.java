package com.tchalanet.server.core.tenant.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.tenant.application.query.model.GetTenantBySlugQuery;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class GetTenantBySlugQueryHandler implements QueryHandler<GetTenantBySlugQuery, Optional<Object>> {

  @Override
  public Optional<Object> handle(GetTenantBySlugQuery query) {
    // TODO: return tenant DTO by slug
    throw new UnsupportedOperationException("GetTenantBySlugQueryHandler not implemented yet");
  }
}

