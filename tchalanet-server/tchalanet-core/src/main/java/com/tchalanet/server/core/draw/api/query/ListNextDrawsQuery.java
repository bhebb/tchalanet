package com.tchalanet.server.core.draw.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.paging.TchPage;
import com.tchalanet.server.core.draw.application.query.projection.DrawSummary;
import org.springframework.data.domain.Pageable;

/**
 * Query pour lister les prochains draws.
 * Remplace GetNextDrawsQuery.
 */
public record ListNextDrawsQuery(
    ResultSlotId resultSlotId,
    int lookaheadHours,
    int limitPerChannel,
    Pageable pageable
) implements Query<TchPage<DrawSummary>> {
}
