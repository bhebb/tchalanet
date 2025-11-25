package com.tchalanet.server.jobs.infra.scheduler;

import com.tchalanet.server.draw.domain.usecase.CloseDueDrawsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawClosingScheduler {

  private final CloseDueDrawsUseCase closeDueDrawsUseCase;

  // run every minute - tune later
  @Scheduled(cron = "0 * * * * *")
  public void closeDueDraws() {
    closeDueDrawsUseCase.execute();
  }
}
