package com.tchalanet.server.core.sales.internal.infra.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SalesReadModelRefreshJob {

  @Scheduled(cron = "0 30 * * * *")
  public void run() {
    log.debug("SalesReadModelRefreshJob tick");
  }
}

