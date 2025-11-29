package com.tchalanet.server.features.stats.infra.listeners;

import com.tchalanet.server.core.pos.infra.events.SessionClosedEvent;
import com.tchalanet.server.features.stats.domain.ports.in.ComputeSessionStatsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionEventListener {

  private final ComputeSessionStatsUseCase computeSessionStatsUseCase;

  @Async
  @TransactionalEventListener
  public void handleSessionClosedEvent(SessionClosedEvent event) {
    log.info(
        "Received SessionClosedEvent for sessionId: {}. Triggering session stats computation.",
        event.getSessionId());
    try {
      // Call a use case to compute and save stats for the closed session
      computeSessionStatsUseCase.computeAndSaveStatsForSession(event.getSessionId());
      log.info("Successfully computed stats for sessionId: {}", event.getSessionId());
    } catch (Exception e) {
      log.error(
          "Failed to compute stats for sessionId: {}. Error: {}",
          event.getSessionId(),
          e.getMessage(),
          e);
    }
  }
}
