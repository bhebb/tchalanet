package com.tchalanet.server.core.draw.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.draw.internal.application.query.projection.DrawSummary;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Query pour lister les derniers tirages avec résultats.
 * Remplace GetLatestDrawsWithResultsQuery.
 */
public record ListLatestDrawsWithResultsQuery(
    List<String> resultSlotKeys,
    Pageable pageable
) implements Query<TchPage<DrawSummary>> {
}
