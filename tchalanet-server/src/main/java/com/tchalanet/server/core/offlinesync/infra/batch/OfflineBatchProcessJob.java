package com.tchalanet.server.core.offlinesync.infra.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OfflineBatchProcessJob {

  @Scheduled(cron = "0 */5 * * * *")
  public void run() {
    log.debug("OfflineBatchProcessJob tick");
  }
}

