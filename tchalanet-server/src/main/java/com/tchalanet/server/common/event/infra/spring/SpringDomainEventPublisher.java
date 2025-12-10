package com.tchalanet.server.common.event.infra.spring;

import com.tchalanet.server.common.event.DomainEvent;
import com.tchalanet.server.common.event.DomainEventPublisher;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringDomainEventPublisher implements DomainEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(SpringDomainEventPublisher.class);

  private final ApplicationEventPublisher delegate;

  public SpringDomainEventPublisher(ApplicationEventPublisher delegate) {
    this.delegate = delegate;
  }

  @Override
  public void publish(DomainEvent event) {
    delegate.publishEvent(event);
    if (log.isDebugEnabled()) {
      log.debug("DomainEvent published: type={} tenant={} id={}", event.eventType(), event.tenantId(), event.eventId());
    }
  }

  @Override
  public void publish(Collection<? extends DomainEvent> events) {
    events.forEach(this::publish);
  }
}

