package com.tchalanet.server.platform.archive.internal.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled cleanup of expired temporary restore runs.
 *
 * <p>Runs daily at 03:30 UTC by default. Safe to run concurrently — the underlying
 * {@link ArchiveRestoreService#cleanupExpired()} is idempotent.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ArchiveRestoreCleanupScheduler {

  private final ArchiveRestoreService restoreService;

  @Scheduled(cron = "0 30 3 * * *", zone = "UTC")
  public void cleanupExpiredRestoreRuns() {
    log.info("archive restore cleanup: scheduled run starting");
    try {
      int cleaned = restoreService.cleanupExpired();
      log.info("archive restore cleanup: scheduled run complete — {} runs cleaned", cleaned);
    } catch (Exception ex) {
      log.error("archive restore cleanup: scheduled run failed: {}", ex.getMessage(), ex);
    }
  }
}
