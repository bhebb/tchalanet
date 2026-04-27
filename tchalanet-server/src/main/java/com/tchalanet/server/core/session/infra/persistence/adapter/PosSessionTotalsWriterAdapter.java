package com.tchalanet.server.core.session.infra.persistence.adapter;

import com.tchalanet.server.core.session.application.port.out.PosSessionTotalsWriterPort;
import com.tchalanet.server.core.session.domain.model.PosSessionTotals;
import com.tchalanet.server.core.session.infra.persistence.mapper.PosSessionTotalsMapper;
import com.tchalanet.server.core.session.infra.persistence.repository.PosSessionTotalsJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PosSessionTotalsWriterAdapter implements PosSessionTotalsWriterPort {

  private final PosSessionTotalsJpaRepository repository;
  private final PosSessionTotalsMapper mapper;

  @Override
  public PosSessionTotals upsert(PosSessionTotals totals) {
    var entity = mapper.toEntity(totals);

    var saved = repository.save(entity);
    return mapper.toDomain(saved);
  }
}
