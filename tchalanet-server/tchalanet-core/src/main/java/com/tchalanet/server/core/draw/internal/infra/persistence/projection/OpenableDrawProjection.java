package com.tchalanet.server.core.draw.internal.infra.persistence.projection;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public interface OpenableDrawProjection {
  UUID getTenantId();

  UUID getDrawId();

  UUID getResultSlotId();

  LocalDate getDrawDate();

  Boolean getLocked();

  Instant getScheduledAt();

  Instant getCutoffAt();
}
