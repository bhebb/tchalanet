package com.tchalanet.server.core.haiti.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.types.id.TchalaEntryId;
import com.tchalanet.server.core.haiti.application.port.out.TchalaEntryRepositoryPort;
import com.tchalanet.server.core.haiti.application.query.model.GetTchalaEntryQuery;
import com.tchalanet.server.core.haiti.domain.tchala.model.TchalaEntry;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public final class GetTchalaEntryQueryHandler
    implements QueryHandler<GetTchalaEntryQuery, Optional<TchalaEntry>> {

  private final TchalaEntryRepositoryPort repo;

  public GetTchalaEntryQueryHandler(TchalaEntryRepositoryPort repo) {
    this.repo = Objects.requireNonNull(repo);
  }

  @Override
  public Optional<TchalaEntry> handle(GetTchalaEntryQuery q) {
    Objects.requireNonNull(q);
    TchalaEntryId id = q.entryId();
    return repo.findById(id);
  }
}
