package com.tchalanet.server.core.user.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.user.application.port.out.UserReaderPort;
import com.tchalanet.server.core.user.application.query.model.PagedListTenantUsersQuery;
import com.tchalanet.server.core.user.domain.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@UseCase
@RequiredArgsConstructor
public class ListTenantUsersQueryHandler
    implements QueryHandler<PagedListTenantUsersQuery, Page<AppUser>> {

  private final UserReaderPort repo;

  @Override
  public Page<AppUser> handle(PagedListTenantUsersQuery query) {
    var pageable = PageRequest.of(query.page(), query.size());
    return repo.findByTenantId(query.tenantId(), pageable);
  }
}
