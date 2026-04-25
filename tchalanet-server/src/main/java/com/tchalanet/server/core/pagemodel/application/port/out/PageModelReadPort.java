package com.tchalanet.server.core.pagemodel.application.port.out;

import com.tchalanet.server.common.types.id.PageModelId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelInstance;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PageModelReadPort {

  // [Phase 3C] UUID → PageModelId (analysis §MAJEUR typed_ids §2)
  Optional<PageModelInstance> findById(PageModelId id);

  /** RLS-scoped: find the PUBLISHED for current tenant context */
  Optional<PageModelInstance> findPublishedByLogicalId(String logicalId);

  /** RLS-scoped: list all PUBLISHED versions for logicalId in current tenant context */
  List<PageModelInstance> findAllPublishedByLogicalId(String logicalId);

  // [Phase 3C] list(UUID...) supprimé — UUID interdit dans les ports (typed_ids §2)
  // [Phase 3B] paged search pour ListPageModelsHandler (analysis §gap — ListPageModelsHandler absent)
  Page<PageModelInstance> search(
      Optional<TenantId> tenantId,
      Optional<String> scope,
      Optional<String> logicalId,
      Pageable pageable
  );
}
