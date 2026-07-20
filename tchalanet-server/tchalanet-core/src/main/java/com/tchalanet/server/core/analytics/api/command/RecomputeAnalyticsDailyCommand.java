package com.tchalanet.server.core.analytics.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.LocalDate;

/**
 * @deprecated Use {@link ReconcileAnalyticsCommand} with {@code REBUILD_AND_VALIDATE}. This command
 *     is disabled because it did not have a post-rebuild verification contract and must not be used
 *     for financial repair.
 * @param tenantId scope to one tenant
 * @param from start date (inclusive)
 * @param to end date (inclusive); max window = 90 days per call
 */
@Deprecated(forRemoval = true)
public record RecomputeAnalyticsDailyCommand(TenantId tenantId, LocalDate from, LocalDate to)
    implements Command<Void> {

  public RecomputeAnalyticsDailyCommand {
    if (tenantId == null) throw new IllegalArgumentException("tenantId must not be null");
    if (from == null || to == null) throw new IllegalArgumentException("from/to must not be null");
    if (from.isAfter(to)) throw new IllegalArgumentException("from must be <= to");
    if (from.plusDays(90).isBefore(to))
      throw new IllegalArgumentException("Recompute window exceeds 90 days; split the request");
  }
}
