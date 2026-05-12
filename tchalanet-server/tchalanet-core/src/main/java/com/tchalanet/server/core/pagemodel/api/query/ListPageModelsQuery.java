package com.tchalanet.server.core.pagemodel.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

// [Phase 3B] UUID → Optional<TenantId> ; Query<Object> → Query<TchPage<PageModelSummaryView>> (analysis §MAJEUR typed_ids §4)
public record ListPageModelsQuery(
    Optional<TenantId> tenantId,
    Optional<String> scope,
    Optional<String> logicalId,
    Pageable pageable
) implements Query<TchPage<PageModelSummaryView>> {}
