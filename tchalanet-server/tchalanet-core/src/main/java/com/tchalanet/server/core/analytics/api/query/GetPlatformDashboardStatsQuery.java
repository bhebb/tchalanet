package com.tchalanet.server.core.analytics.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.analytics.api.model.PlatformDashboardStatsView;

import java.time.LocalDate;

/**
 * Returns aggregated analytics KPIs for the platform / super-admin dashboard.
 *
 * @param from           start of the date window (inclusive, UTC)
 * @param to             end of the date window (inclusive, UTC)
 * @param topTenantsLimit max tenant-rank rows to include (default 5)
 */
public record GetPlatformDashboardStatsQuery(
    LocalDate from,
    LocalDate to,
    int       topTenantsLimit
) implements Query<PlatformDashboardStatsView> {

  public static GetPlatformDashboardStatsQuery today(LocalDate today) {
    return new GetPlatformDashboardStatsQuery(today, today, 5);
  }
}
