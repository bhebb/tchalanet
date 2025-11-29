package com.tchalanet.server.jobs.infra.scheduler;

import com.tchalanet.server.draw.application.command.handler.SettleDrawsUseCase;
import com.tchalanet.server.draw.application.command.model.SettleDrawCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrawSettlementScheduler {

  private final SettleDrawsUseCase settleDrawsUseCase;

  // run every minute - tune later
  @Scheduled(cron = "30 * * * * *")
  public void settleDraws() {

    // fixme tenantId
    settleDrawsUseCase.handle(new SettleDrawCommand(null, null));
  }
}
