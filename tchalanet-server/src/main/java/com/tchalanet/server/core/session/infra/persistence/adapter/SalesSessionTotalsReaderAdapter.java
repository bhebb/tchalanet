package com.tchalanet.server.core.session.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.core.session.application.port.out.SalesSessionTotalsReaderPort;
import com.tchalanet.server.core.session.domain.model.SalesSessionTotals;
import com.tchalanet.server.core.session.infra.persistence.mapper.SalesSessionTotalsMapper;
import com.tchalanet.server.core.session.infra.persistence.repository.SalesSessionTotalsJpaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SalesSessionTotalsReaderAdapter implements SalesSessionTotalsReaderPort {

  private final SalesSessionTotalsJpaRepository repository;
  private final SalesSessionTotalsMapper mapper;

  @Override
  public Optional<SalesSessionTotals> findBySessionId(SessionId sessionId) {
    return repository.findBySessionId(sessionId.uuid()).map(mapper::toDomain);
  }
}
