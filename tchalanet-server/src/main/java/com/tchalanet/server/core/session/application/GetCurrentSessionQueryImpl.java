package com.tchalanet.server.core.session.application;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.session.domain.model.PosSession;
import com.tchalanet.server.core.session.domain.ports.in.GetCurrentSessionQuery;
import com.tchalanet.server.core.session.domain.ports.out.PosSessionRepositoryPort;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetCurrentSessionQueryImpl implements GetCurrentSessionQuery {

  private final PosSessionRepositoryPort sessionRepository;

  @Override
  public Optional<PosSession> get(UUID tenantId, UUID terminalId) {
    return sessionRepository.findOpenByTerminal(tenantId, terminalId);
  }
}
