package com.tchalanet.server.stats.infra.listeners;

import com.tchalanet.server.stats.domain.ports.in.UpdateRealtimeStatsOnTicketCreatedUseCase;
import com.tchalanet.server.ticket.infra.events.TicketCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketEventListener {

  private final UpdateRealtimeStatsOnTicketCreatedUseCase
      updateRealtimeStatsUseCase; // Inject the use case

  @Async
  @TransactionalEventListener
  public void handleTicketCreatedEvent(TicketCreatedEvent event) {
    log.info(
        "Received TicketCreatedEvent for ticketId: {}. Updating real-time stats (if configured).",
        event.getTicketId());
    try {
      updateRealtimeStatsUseCase.updateStats(
          event.getTicketId(), event.getTenantId(), event.getSessionId());
      log.info("Successfully processed real-time stats for ticketId: {}", event.getTicketId());
    } catch (Exception e) {
      log.error(
          "Failed to update real-time stats for ticketId: {}. Error: {}",
          event.getTicketId(),
          e.getMessage(),
          e);
    }
  }
}
