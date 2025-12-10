package com.tchalanet.server.common.event;

import java.util.Collection;

public interface DomainEventPublisher {
  void publish(DomainEvent event);

  void publish(Collection<? extends DomainEvent> events);
}

