package com.tchalanet.server.common.event.infra.spring;

import com.tchalanet.server.common.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "stg"})
public class LoggingDomainEventListener {

  private static final Logger log = LoggerFactory.getLogger(LoggingDomainEventListener.class);

  @EventListener
  public void on(DomainEvent event) {
    log.info(
        "DomainEvent caught (dev): type={} tenant={} id={}",
        event.eventType(),
        event.tenantId(),
        event.eventId().value());
  }
}
