package com.tchalanet.server.core.tenantuser.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.tenantuser.application.query.model.PagedListTenantUsersQuery;
import com.tchalanet.server.core.tenantuser.application.query.model.TenantUserRow;
import com.tchalanet.server.core.tenantuser.application.port.out.TenantUserReaderPort;
import lombok.RequiredArgsConstructor;
import com.tchalanet.server.common.web.paging.TchPage;

@UseCase
@RequiredArgsConstructor
public class PagedListTenantUsersQueryHandler implements QueryHandler<PagedListTenantUsersQuery, TchPage<TenantUserRow>> {

  private final TenantUserReaderPort reader;

  @Override
  public TchPage<TenantUserRow> handle(PagedListTenantUsersQuery query) {
    return reader.pagedListByTenant(query.tenantId(), query.pageRequest());
  }
}
