package com.tchalanet.server.core.sales.infra.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TicketResultingJob {

  @Scheduled(cron = "0 */10 * * * *")
  public void run() {
    log.debug("TicketResultingJob tick");
  }
}

