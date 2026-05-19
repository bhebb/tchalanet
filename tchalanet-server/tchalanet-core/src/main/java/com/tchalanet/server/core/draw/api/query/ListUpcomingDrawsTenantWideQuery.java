package com.tchalanet.server.core.draw.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.draw.internal.application.query.projection.DrawSummary;

import java.util.List;

/**
 * Lists the upcoming draws ({@code SCHEDULED}/{@code OPEN}) across all channels of the
 * current tenant within the next {@code lookaheadHours} hours. Tenant scope is inferred
 * from the request context (RLS-filtered).
 *
 * <p>Used notably by {@code core.offlinesync} when issuing a grant: the response embeds the
 * list of draws the cashier is allowed to sell offline against, so the POS device can pin
 * a {@code drawId} on each offline sale.
 *
 * @param lookaheadHours  forward window (typically days × 24)
 * @param limit           hard cap on the returned list size (1..200 enforced by the handler)
 */
public record ListUpcomingDrawsTenantWideQuery(
    int lookaheadHours,
    int limit
) implements Query<List<DrawSummary>> {}
