package com.tchalanet.server.core.sales.infra.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TicketArchiveJob {

  @Scheduled(cron = "0 0 * * * *")
  public void run() {
    log.debug("TicketArchiveJob tick");
  }
}

