package com.tchalanet.server.core.draw.infra.persistence;

import java.util.UUID;

public interface DrawDueToCloseProjection {
  UUID getTenantId();

  UUID getDrawId();

  Boolean getLocked();
}
