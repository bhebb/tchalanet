package com.tchalanet.server.core.analytics.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.analytics.api.model.TenantDashboardStatsView;

import java.time.LocalDate;

/**
 * Returns analytics KPIs for the tenant-admin dashboard.
 *
 * <p>Executed via {@code QueryBus}; never direct SQL from features.
 *
 * @param tenantId   the tenant to scope the query to
 * @param from       start of the date window (inclusive, tenant-local)
 * @param to         end of the date window (inclusive, tenant-local)
 * @param topGamesLimit max number of game-breakdown rows to return (default 5)
 */
public record GetTenantDashboardStatsQuery(
    TenantId  tenantId,
    LocalDate from,
    LocalDate to,
    int       topGamesLimit
) implements Query<TenantDashboardStatsView> {

  /** Convenience factory for today-only range. */
  public static GetTenantDashboardStatsQuery today(TenantId tenantId, LocalDate today) {
    return new GetTenantDashboardStatsQuery(tenantId, today, today, 5);
  }
}
