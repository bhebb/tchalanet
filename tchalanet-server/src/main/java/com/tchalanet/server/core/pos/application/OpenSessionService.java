package com.tchalanet.server.core.pos.application;

import com.tchalanet.server.core.accesscontrol.application.annotation.RequiresPermission;
import com.tchalanet.server.core.pos.domain.model.PosSession;
import com.tchalanet.server.core.pos.domain.ports.in.OpenSessionUseCase;
import com.tchalanet.server.core.pos.domain.ports.out.PosSessionEventPublisherPort;
import com.tchalanet.server.core.pos.domain.ports.out.PosSessionRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenSessionService implements OpenSessionUseCase {

  private final PosSessionRepositoryPort sessionRepository;
  private final PosSessionEventPublisherPort eventPublisher;

  // private final AccessCheckerPort accessChecker; // No longer directly injected here, handled by
  // annotation

  @Override
  @Transactional
  @RequiresPermission("session.open") // Apply the annotation
  public PosSession openSession(OpenSessionCommand command) {
    // Rule: Check that there isn't already an OPEN session for this terminal
    sessionRepository
        .findOpenSessionByTerminal(command.tenantId(), command.terminalId())
        .ifPresent(
            s -> {
              throw new IllegalStateException(
                  "Terminal "
                      + command.terminalId()
                      + " already has an OPEN session: "
                      + s.getId());
            });

    PosSession newSession =
        PosSession.open(
            command.tenantId(), command.terminalId(), command.userId(), command.openingFloat());

    PosSession savedSession = sessionRepository.save(newSession);
    eventPublisher.publishSessionOpenedEvent(
        savedSession.getId(),
        savedSession.getTenantId(),
        savedSession.getTerminalId(),
        savedSession.getUserId());

    log.info(
        "POS Session {} opened for terminal {} by user {}",
        savedSession.getId(),
        savedSession.getTerminalId(),
        savedSession.getUserId());
    return savedSession;
  }
}
