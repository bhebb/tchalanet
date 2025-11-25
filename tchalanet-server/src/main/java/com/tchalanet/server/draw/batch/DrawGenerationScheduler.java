package com.tchalanet.server.draw.batch;

import com.tchalanet.server.draw.domain.usecase.GenerateUpcomingDrawsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawGenerationScheduler {

  private final GenerateUpcomingDrawsUseCase generator;

  @Value("${draw.generation.days:14}")
  private int days;

  // default: run daily at 00:05
  @Scheduled(cron = "0 5 0 * * *")
  public void runDailyGeneration() {
    log.info("Running draw generation for next {} days", days);
    generator.generateForNextDays(days);
  }
}
