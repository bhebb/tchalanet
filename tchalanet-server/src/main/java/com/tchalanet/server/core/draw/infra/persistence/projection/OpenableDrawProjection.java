package com.tchalanet.server.core.draw.infra.persistence.projection;

import java.time.Instant;
import java.util.UUID;

public interface OpenableDrawProjection {
  UUID getTenantId();

  UUID getDrawId();

  Boolean getLocked();

  Instant getScheduledAt();

  Instant getCutoffAt();
}
