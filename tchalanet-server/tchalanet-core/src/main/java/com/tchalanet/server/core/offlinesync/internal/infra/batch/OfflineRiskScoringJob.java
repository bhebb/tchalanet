package com.tchalanet.server.core.offlinesync.internal.infra.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OfflineRiskScoringJob {

  @Scheduled(cron = "0 15 * * * *")
  public void run() {
    log.debug("OfflineRiskScoringJob tick");
  }
}

