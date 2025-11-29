package com.tchalanet.server.core.pos.infra.listeners;

import com.tchalanet.server.core.pos.domain.ports.out.PosSessionRepositoryPort;
import com.tchalanet.server.core.sales.infra.events.TicketCreatedEvent;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class PosSessionActivityListener {

  private final PosSessionRepositoryPort posSessionRepository;

  @Async
  @TransactionalEventListener // Defaults to AFTER_COMMIT
  public void onTicketCreated(TicketCreatedEvent event) {
    log.debug(
        "Received TicketCreatedEvent for ticket {} in session {}. Updating session activity.",
        event.getTicketId(),
        event.getSessionId());
    posSessionRepository
        .findById(event.getSessionId())
        .ifPresent(
            session -> {
              session.recordActivity(Instant.now()); // Update lastActivityAt
              posSessionRepository.save(session);
              log.trace("POS Session {} last activity updated.", session.getId());
            });
  }
}
