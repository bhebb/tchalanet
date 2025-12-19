package com.tchalanet.server.core.draw.infra.persistence;

import java.time.Instant;
import java.util.UUID;

public interface DrawOpenableProjection {
  UUID getTenantId();

  UUID getDrawId();

  Boolean getLocked();

  Instant getScheduledAt();

  Integer getCutoffSec();
}
