package com.tchalanet.server.core.haiti.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.haiti.internal.application.port.out.TchalaEntryRepositoryPort;
import com.tchalanet.server.core.haiti.api.query.GetTchalaByNumberQuery;
import com.tchalanet.server.core.haiti.internal.domain.tchala.model.TchalaEntry;
import com.tchalanet.server.core.haiti.internal.domain.tchala.model.TchalaLang;
import com.tchalanet.server.core.haiti.internal.domain.tchala.model.TchalaNumber;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public final class GetTchalaByNumberQueryHandler
    implements QueryHandler<GetTchalaByNumberQuery, TchPage<TchalaEntry>> {

  private final TchalaEntryRepositoryPort repo;

  public GetTchalaByNumberQueryHandler(TchalaEntryRepositoryPort repo) {
    this.repo = Objects.requireNonNull(repo);
  }

  @Override
  public TchPage<TchalaEntry> handle(GetTchalaByNumberQuery q) {
    Objects.requireNonNull(q);
    var lang = TchalaLang.of(q.lang());
    var number = TchalaNumber.of(q.number());
    int page = Math.max(0, q.page());
    int size = Math.max(1, q.size());
    return repo.findApprovedByNumber(lang, number, page, size);
  }
}
