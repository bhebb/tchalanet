package com.tchalanet.server.core.haiti.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.haiti.application.port.out.TchalaEntryRepositoryPort;
import com.tchalanet.server.core.haiti.application.query.model.SearchTchalaQuery;
import com.tchalanet.server.core.haiti.domain.tchala.model.TchalaEntry;
import com.tchalanet.server.core.haiti.domain.tchala.model.TchalaLang;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public final class SearchTchalaQueryHandler
    implements QueryHandler<SearchTchalaQuery, TchPage<TchalaEntry>> {

  private final TchalaEntryRepositoryPort repo;

  public SearchTchalaQueryHandler(TchalaEntryRepositoryPort repo) {
    this.repo = Objects.requireNonNull(repo);
  }

  @Override
  public TchPage<TchalaEntry> handle(SearchTchalaQuery q) {
    Objects.requireNonNull(q);
    var lang = TchalaLang.of(q.lang());
    int page = Math.max(0, q.page());
    int size = Math.max(1, q.size());

    return repo.searchApproved(lang, q.text(), page, size);
  }
}
