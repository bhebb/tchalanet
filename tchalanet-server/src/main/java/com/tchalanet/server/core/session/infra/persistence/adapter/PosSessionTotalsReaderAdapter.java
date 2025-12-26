package com.tchalanet.server.core.session.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.SessionId;

import com.tchalanet.server.core.session.application.port.out.PosSessionTotalsReaderPort;
import com.tchalanet.server.core.session.domain.model.PosSessionTotals;
import com.tchalanet.server.core.session.infra.persistence.mapper.PosSessionTotalsMapper;
import com.tchalanet.server.core.session.infra.persistence.repository.PosSessionTotalsJpaRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PosSessionTotalsReaderAdapter implements PosSessionTotalsReaderPort {

  private final PosSessionTotalsJpaRepository repository;
  private final PosSessionTotalsMapper mapper;

  @Override
  public Optional<PosSessionTotals> findBySessionId(SessionId sessionId) {
    return repository.findBySessionId(sessionId.uuid())
        .map(mapper::toDomain);
  }
}
