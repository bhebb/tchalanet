package com.tchalanet.server.features.tenantadmin.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.features.tenantadmin.application.query.model.PagedListTenantUsersQuery;
import com.tchalanet.server.features.tenantadmin.application.query.model.TenantUserRow;
import com.tchalanet.server.features.tenantadmin.application.port.out.TenantAdminReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class PagedListTenantUsersQueryHandler implements QueryHandler<PagedListTenantUsersQuery, TchPage<TenantUserRow>> {

  private final TenantAdminReaderPort reader;

  @Override
  public TchPage<TenantUserRow> handle(PagedListTenantUsersQuery query) {
    return reader.pagedListByTenant(query.tenantId(), query.pageRequest());
  }
}
