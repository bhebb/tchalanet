package com.tchalanet.server.core.analytics.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.LocalDate;

/**
 * Triggers a full recompute of {@code analytics_daily} from source-of-truth data
 * for the given date window (and optionally a single tenant).
 *
 * <p>Recompute rebuilds rows from sales/settlement/payout source tables,
 * not from processed-event log. Idempotent — safe to re-run.
 *
 * @param tenantId  scope to one tenant; {@code null} = all tenants (SUPER_ADMIN only)
 * @param from      start date (inclusive)
 * @param to        end date (inclusive); max window = 90 days per call
 */
public record RecomputeAnalyticsDailyCommand(
    TenantId  tenantId,
    LocalDate from,
    LocalDate to
) implements Command<Void> {

  public RecomputeAnalyticsDailyCommand {
    if (from == null || to == null) throw new IllegalArgumentException("from/to must not be null");
    if (from.isAfter(to))          throw new IllegalArgumentException("from must be <= to");
    if (from.plusDays(90).isBefore(to))
      throw new IllegalArgumentException("Recompute window exceeds 90 days; split the request");
  }
}
