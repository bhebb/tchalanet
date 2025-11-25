package com.tchalanet.server.jobs.infra.scheduler;

import com.tchalanet.server.draw.domain.usecase.SettleDrawsUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DrawSettlementScheduler {

  private final SettleDrawsUseCase settleDrawsUseCase;

  public DrawSettlementScheduler(SettleDrawsUseCase settleDrawsUseCase) {
    this.settleDrawsUseCase = settleDrawsUseCase;
  }

  // run every minute - tune later
  @Scheduled(cron = "30 * * * * *")
  public void settleDraws() {
    settleDrawsUseCase.execute();
  }
}
