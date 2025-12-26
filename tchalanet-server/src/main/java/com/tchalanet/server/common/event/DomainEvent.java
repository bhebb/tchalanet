package com.tchalanet.server.common.event;

import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {
  UUID eventId();

  Instant occurredAt();

  TenantId tenantId();

  default String eventType() {
    return getClass().getSimpleName();
  }
}
