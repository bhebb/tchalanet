package com.tchalanet.server.core.pagemodel.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PageModelReadPort {

  Optional<PageModelProjection> findPublishedByLogicalId(String logicalId);

  Optional<PageModelProjection> findById(UUID id);

  List<PageModelProjection> list(UUID tenantIdOrNull, String scopeOrNull, String logicalIdOrNull);

  // Projection record (can be moved to infra.persistence)
  public static interface PageModelProjection {
    UUID id();
    String logicalId();
    int schemaVersion();
    com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc doc();
  }
}

