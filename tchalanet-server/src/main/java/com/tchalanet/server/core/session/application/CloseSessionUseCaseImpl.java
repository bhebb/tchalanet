package com.tchalanet.server.core.session.application;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.session.domain.model.PosSession;
import com.tchalanet.server.core.session.domain.ports.in.CloseSessionUseCase;
import com.tchalanet.server.core.session.domain.ports.out.PosSessionRepositoryPort;
import java.time.Instant;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CloseSessionUseCaseImpl implements CloseSessionUseCase {

  private final PosSessionRepositoryPort sessionRepository;

  @Override
  public PosSession close(Command command) {
    PosSession existing =
        sessionRepository
            .findById(command.sessionId())
            .orElseThrow(
                () -> new IllegalStateException("PosSession not found: " + command.sessionId()));

    PosSession closed = existing.close(command.closingAmount(), Instant.now());
    return sessionRepository.save(closed);
  }
}
