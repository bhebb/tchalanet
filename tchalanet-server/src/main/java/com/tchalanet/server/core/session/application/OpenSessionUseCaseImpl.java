package com.tchalanet.server.core.session.application;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.session.domain.model.PosSession;
import com.tchalanet.server.core.session.domain.ports.in.OpenSessionUseCase;
import com.tchalanet.server.core.session.domain.ports.out.PosSessionRepositoryPort;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class OpenSessionUseCaseImpl implements OpenSessionUseCase {

  private final PosSessionRepositoryPort sessionRepository;

  @Override
  public PosSession open(Command command) {
    // Vérifier qu'il n'y a pas déjà une session OPEN pour ce tenant/terminal
    sessionRepository
        .findOpenByTerminal(command.tenantId(), command.terminalId())
        .ifPresent(
            existing -> {
              throw new IllegalStateException(
                  "A POS session is already OPEN for tenant="
                      + command.tenantId()
                      + " and terminal="
                      + command.terminalId());
            });

    PosSession session =
        PosSession.open(
            UUID.randomUUID(),
            command.tenantId(),
            command.outletId(),
            command.terminalId(),
            command.userId(),
            command.openingFloat(),
            Instant.now());

    return sessionRepository.save(session);
  }
}
