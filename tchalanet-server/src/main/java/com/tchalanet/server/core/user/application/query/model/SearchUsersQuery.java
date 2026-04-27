package com.tchalanet.server.core.user.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import java.time.Instant;
import org.springframework.data.domain.PageRequest;

public record SearchUsersQuery(
    String nameLike,
    String status,
    Instant createdAfter,
    Instant createdBefore,
    TchPageRequest pageRequest)
    implements Query<TchPage<UserRow>> {

  // Backward-compatible constructor: accept (page, size) ints and build a TchPageRequest
  public SearchUsersQuery(
      String nameLike, String status, Instant createdAfter, Instant createdBefore, int page, int size) {
    this(nameLike, status, createdAfter, createdBefore, new TchPageRequest(PageRequest.of(page, size)));
  }
}
