package com.tchalanet.server.core.draw.api.query;

import com.tchalanet.server.common.bus.Query;
import java.time.Instant;
import java.time.LocalDate;

/** Returns exact generated-draw KPI counts for a business-date range. */
public record GetDrawsSummaryQuery(DrawSearchCriteria criteria, LocalDate today, Instant now)
    implements Query<DrawsSummary> {}
