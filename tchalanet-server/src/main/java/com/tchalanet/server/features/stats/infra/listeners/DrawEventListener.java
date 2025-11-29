package com.tchalanet.server.features.stats.infra.listeners;

import com.tchalanet.server.core.draw.domain.events.DrawResultedEvent;
import com.tchalanet.server.features.stats.domain.ports.in.ComputeDrawStatsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawEventListener {

  private final ComputeDrawStatsUseCase computeDrawStatsUseCase;

  @Async
  @TransactionalEventListener // Defaults to AFTER_COMMIT
  public void handleDrawResultedEvent(DrawResultedEvent event) {
    log.info(
        "Received DrawResultedEvent for drawId: {}. Triggering stats computation.",
        event.getDrawId());
    try {
      computeDrawStatsUseCase.computeAndSaveStatsForDraw(event.getDrawId());
      log.info("Successfully computed stats for drawId: {}", event.getDrawId());
    } catch (Exception e) {
      log.error(
          "Failed to compute stats for drawId: {}. Error: {}",
          event.getDrawId(),
          e.getMessage(),
          e);
      // In a production system, this might trigger an alert or a retry mechanism.
    }
  }
}
