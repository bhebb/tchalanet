package com.tchalanet.server.core.pagemodel.api.query;

import com.tchalanet.server.common.types.id.PageModelId;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelStatus;
import java.time.Instant;

// [Phase 3B] projection de liste pour ListPageModelsHandler (analysis §gap — Query<Object> non typé)
public record PageModelSummaryView(
    PageModelId id,
    String logicalId,
    String scope,
    String slug,
    PageModelStatus status,
    int schemaVersion,
    Instant updatedAt
) {}

