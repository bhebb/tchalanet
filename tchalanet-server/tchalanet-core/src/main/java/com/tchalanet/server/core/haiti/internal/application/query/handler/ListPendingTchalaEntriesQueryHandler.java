package com.tchalanet.server.core.haiti.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.paging.TchPage;
import com.tchalanet.server.core.haiti.application.port.out.TchalaEntryRepositoryPort;
import com.tchalanet.server.core.haiti.application.query.model.ListPendingTchalaEntriesQuery;
import com.tchalanet.server.core.haiti.domain.tchala.model.TchalaEntry;
import com.tchalanet.server.core.haiti.domain.tchala.model.TchalaLang;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public final class ListPendingTchalaEntriesQueryHandler
    implements QueryHandler<ListPendingTchalaEntriesQuery, TchPage<TchalaEntry>> {

  private final TchalaEntryRepositoryPort repo;

  public ListPendingTchalaEntriesQueryHandler(TchalaEntryRepositoryPort repo) {
    this.repo = Objects.requireNonNull(repo, "repo");
  }

  @Override
  public TchPage<TchalaEntry> handle(ListPendingTchalaEntriesQuery q) {
    Objects.requireNonNull(q, "query");
    var lang = TchalaLang.of(q.lang());
    int page = Math.max(0, q.page());
    int size = Math.max(1, q.size());
    return repo.listPending(lang, q.conflictOnly(), page, size);
  }
}
