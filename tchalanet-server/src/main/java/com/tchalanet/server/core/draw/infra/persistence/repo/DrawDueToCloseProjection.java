package com.tchalanet.server.core.draw.infra.persistence.repo;

import java.util.UUID;

public interface DrawDueToCloseProjection {
  UUID getTenantId();

  UUID getDrawId();

  Boolean getLocked();
}
