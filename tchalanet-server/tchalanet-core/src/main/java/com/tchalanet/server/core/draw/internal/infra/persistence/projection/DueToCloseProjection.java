package com.tchalanet.server.core.draw.internal.infra.persistence.projection;

import java.util.UUID;

public interface DueToCloseProjection {
  UUID getTenantId();

  UUID getDrawId();

  Boolean getLocked();
}
