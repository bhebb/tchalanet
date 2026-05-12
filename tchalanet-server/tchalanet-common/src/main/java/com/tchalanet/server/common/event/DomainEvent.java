package com.tchalanet.server.common.event;

import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;

public interface DomainEvent {
  EventId eventId();

  Instant occurredAt();

  TenantId tenantId();

  default String eventType() {
    return getClass().getSimpleName();
  }
}
