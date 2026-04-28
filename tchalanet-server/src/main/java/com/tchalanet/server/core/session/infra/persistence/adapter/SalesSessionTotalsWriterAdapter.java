package com.tchalanet.server.core.session.infra.persistence.adapter;

import com.tchalanet.server.core.session.application.port.out.SalesSessionTotalsWriterPort;
import com.tchalanet.server.core.session.domain.model.SalesSessionTotals;
import com.tchalanet.server.core.session.infra.persistence.mapper.SalesSessionTotalsMapper;
import com.tchalanet.server.core.session.infra.persistence.repository.SalesSessionTotalsJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SalesSessionTotalsWriterAdapter implements SalesSessionTotalsWriterPort {

  private final SalesSessionTotalsJpaRepository repository;
  private final SalesSessionTotalsMapper mapper;

  @Override
  public SalesSessionTotals upsert(SalesSessionTotals totals) {
    var entity = mapper.toEntity(totals);

    var saved = repository.save(entity);
    return mapper.toDomain(saved);
  }
}
