package com.tchalanet.server.core.draw.api.query;

import com.tchalanet.server.common.bus.Query;

import java.util.List;

/**
 * Lists the upcoming draws ({@code SCHEDULED}/{@code OPEN}) across all channels of the
 * current tenant within the next {@code lookaheadHours} hours. Tenant scope is inferred
 * from the request context (RLS-filtered).
 *
 * <p>Used by sales surfaces that need channel and game metadata to validate a sale.
 *
 * @param lookaheadHours  forward window (typically days × 24)
 * @param limit           hard cap on the returned list size (1..200 enforced by the handler)
 */
public record ListUpcomingDrawsTenantWideQuery(
    int lookaheadHours,
    int limit
) implements Query<List<DrawSummary>> {}
