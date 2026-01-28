package com.tchalanet.server.features.tenantadmin.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.features.tenantadmin.application.query.model.GetTenantUserDetailsQuery;
import com.tchalanet.server.features.tenantadmin.application.query.model.TenantUserDetails;
import com.tchalanet.server.features.tenantadmin.application.port.out.TenantAdminReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetTenantUserDetailsQueryHandler implements QueryHandler<GetTenantUserDetailsQuery, TenantUserDetails> {

  private final TenantAdminReaderPort reader;

  @Override
  public TenantUserDetails handle(GetTenantUserDetailsQuery query) {
    return reader.getDetails(query.tenantId(), query.userId());
  }
}
