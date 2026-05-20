package com.tchalanet.server.core.draw.internal.infra.persistence.repo;

import java.time.Instant;
import java.util.UUID;

public interface DrawOpenableProjection {
  UUID getTenantId();

  UUID getDrawId();

  Boolean getLocked();

  Instant getScheduledAt();

  Integer getCutoffSec();
}
