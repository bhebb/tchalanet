package com.tchalanet.server.core.limitpolicy.application.query.handler;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitDefinitionReaderPort;
import com.tchalanet.server.core.limitpolicy.application.query.model.GetLimitDefinitionsQuery;
import com.tchalanet.server.core.limitpolicy.application.query.model.GetLimitDefinitionsResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@Component
@RequiredArgsConstructor
public class GetLimitDefinitionsQueryHandler implements QueryHandler<GetLimitDefinitionsQuery, GetLimitDefinitionsResult> {

  private final LimitDefinitionReaderPort reader;

  @Override
  public GetLimitDefinitionsResult handle(GetLimitDefinitionsQuery query) {
    var definitions = reader.findActiveByTenantId(query.tenantId());
    return new GetLimitDefinitionsResult(definitions);
  }
}
