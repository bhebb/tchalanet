package com.tchalanet.server.core.user.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.user.application.query.model.SearchUsersQuery;
import com.tchalanet.server.core.user.application.query.model.UserRow;
import com.tchalanet.server.core.user.application.port.out.UserReaderPort;
import com.tchalanet.server.core.user.domain.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@RequiredArgsConstructor
public class SearchUsersQueryHandler implements QueryHandler<SearchUsersQuery, TchPage<UserRow>> {

  private final UserReaderPort userReaderPort;

  @Override
  public TchPage<UserRow> handle(SearchUsersQuery query) {
    var pageable = query.pageRequest().pageable();
    var page = userReaderPort.searchByCriteria(query.nameLike(), query.status(), query.createdAfter(), query.createdBefore(), pageable);
    var items = page.getContent().stream().map(u -> new UserRow(u.getId(), u.getKeycloakSub(), u.getUsername(), u.getEmail(), u.getFirstName(), u.getLastName(), u.getDisplayName(), u.getStatus().name())).toList();
    return TchPage.of(items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast(), page.hasNext(), page.hasPrevious());
  }
}
