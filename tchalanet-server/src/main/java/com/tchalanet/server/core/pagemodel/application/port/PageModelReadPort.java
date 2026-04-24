package com.tchalanet.server.core.pagemodel.application.port;

import com.tchalanet.server.core.pagemodel.domain.model.PageModelInstance;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PageModelReadPort {

  Optional<PageModelInstance> findById(UUID id);

  /** RLS-scoped: find the PUBLISHED for current tenant context */
  Optional<PageModelInstance> findPublishedByLogicalId(String logicalId);

  /** RLS-scoped: list all PUBLISHED versions for logicalId in current tenant context */
  List<PageModelInstance> findAllPublishedByLogicalId(String logicalId);

  /** Admin list (may be tenantId filter, but still under admin RLS/context rules) */
  List<PageModelInstance> list(UUID tenantIdOrNull, String scopeOrNull, String logicalIdOrNull);
}

