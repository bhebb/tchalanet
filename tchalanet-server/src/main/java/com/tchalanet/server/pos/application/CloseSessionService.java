package com.tchalanet.server.pos.application;

import com.tchalanet.server.accesscontrol.application.annotation.RequiresPermission; // New import
import com.tchalanet.server.pos.domain.model.PosSession;
import com.tchalanet.server.pos.domain.ports.in.CloseSessionUseCase;
import com.tchalanet.server.pos.domain.ports.out.PosSessionEventPublisherPort;
import com.tchalanet.server.pos.domain.ports.out.PosSessionRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloseSessionService implements CloseSessionUseCase {

  private final PosSessionRepositoryPort sessionRepository;
  private final PosSessionEventPublisherPort eventPublisher;

  // private final AccessCheckerPort accessChecker; // No longer directly injected here, handled by
  // annotation
  // private final TicketAggregationPort ticketAggregationPort; // To aggregate tickets for the
  // session

  @Override
  @Transactional
  @RequiresPermission("session.close") // Apply the annotation
  public PosSession closeSession(CloseSessionCommand command) {
    PosSession session =
        sessionRepository
            .findById(command.sessionId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException("POS Session not found: " + command.sessionId()));

    if (!session.getTenantId().equals(command.tenantId())) {
      throw new SecurityException("Tenant mismatch for session " + command.sessionId());
    }

    // Aggregate tickets for the session (placeholder)
    // var aggregates = ticketAggregationPort.aggregateForSession(session.getId());
    // session.updateAggregates(aggregates); // Assuming a method on PosSession to update totals

    session.close(command.closingAmount()); // Business logic in the domain model

    PosSession savedSession = sessionRepository.save(session);
    eventPublisher.publishSessionClosedEvent(
        savedSession.getId(),
        savedSession.getTenantId(),
        savedSession.getTerminalId(),
        savedSession.getUserId(),
        "MANUAL");

    log.info(
        "POS Session {} closed manually for terminal {} by user {}",
        savedSession.getId(),
        savedSession.getTerminalId(),
        savedSession.getUserId());
    return savedSession;
  }
}
