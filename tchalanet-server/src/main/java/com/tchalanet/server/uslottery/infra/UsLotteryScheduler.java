package com.tchalanet.server.uslottery.infra;

import com.tchalanet.server.uslottery.domain.ports.in.RefreshUsLotteryResultsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Job planifié qui rafraîchit périodiquement les résultats US Lottery (NY/Florida). */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    prefix = "tch.us-lottery",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class UsLotteryScheduler {

  private final RefreshUsLotteryResultsUseCase refreshUseCase;

  /**
   * Rafraîchit périodiquement les résultats US Lottery. Cron configurable via
   * SPRING_TASK_SCHEDULING / application.yaml au besoin.
   */
  @Scheduled(cron = "0 */2 * * * *") // toutes les 2 minutes
  public void refreshLatestResults() {
    log.debug(
        "uslottery-scheduler: triggering RefreshUsLotteryResultsUseCase.refreshAllProviders()");
    refreshUseCase.refresh();
  }
}
