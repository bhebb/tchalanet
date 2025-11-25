package com.tchalanet.server.draw.batch;

import com.tchalanet.server.draw.domain.usecase.CloseDueDrawsUseCase;
import com.tchalanet.server.draw.domain.usecase.RefreshPublicDrawCacheUseCase;
import com.tchalanet.server.draw.domain.usecase.SettleDrawsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawBatchScheduler {

  private final CloseDueDrawsUseCase closeDueDraws;
  private final SettleDrawsUseCase settleDraws;
  private final RefreshPublicDrawCacheUseCase refreshCache;

  // run every minute
  @Scheduled(cron = "0 * * * * *")
  public void runScheduledJobs() {
    log.info("DrawBatchScheduler running: closeDueDraws");
    try {
      closeDueDraws.execute();
    } catch (Exception e) {
      log.error("closeDueDraws failed", e);
    }

    log.info("DrawBatchScheduler running: settleDraws");
    try {
      settleDraws.execute();
    } catch (Exception e) {
      log.error("settleDraws failed", e);
    }

    log.info("DrawBatchScheduler running: refreshCache");
    try {
      refreshCache.execute();
    } catch (Exception e) {
      log.error("refreshCache failed", e);
    }
  }
}
