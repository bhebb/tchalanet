package com.tchalanet.server.platform.idempotence.internal.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Scheduled cleanup of old {@code processed_event} rows. */
@Component
@ConditionalOnProperty(
    prefix = "tch.idempotence.processed-event-cleanup",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class ProcessedEventCleanupScheduler {

  private final ProcessedEventCleanupService cleanupService;

  @Scheduled(cron = "${tch.idempotence.processed-event-cleanup.cron:0 45 4 * * SUN}", zone = "UTC")
  public void cleanupExpiredProcessedEvents() {
    log.info("processed_event cleanup: scheduled run starting");
    try {
      int deleted = cleanupService.cleanupExpired();
      log.info("processed_event cleanup: scheduled run complete - {} rows deleted", deleted);
    } catch (Exception ex) {
      log.error("processed_event cleanup: scheduled run failed: {}", ex.getMessage(), ex);
    }
  }
}
