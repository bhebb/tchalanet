package com.tchalanet.server.core.analytics.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.core.analytics.api.model.PurgeAnalyticsResult;

/**
 * Purge analytics data older than the configured retention window.
 *
 * <p>Two retention targets:
 * <ul>
 *   <li>{@code analytics_daily} — 24 months default.</li>
 *   <li>{@code analytics_draw}  — 24 months default.</li>
 * </ul>
 *
 * <p>Invoked by {@code AnalyticsMaintenanceScheduler}; may also be triggered manually
 * by an ops admin via {@code POST /platform/ops/analytics/purge}.
 *
 * @param dryRun if {@code true}, counts rows to be deleted without actually deleting them
 */
public record PurgeAnalyticsCommand(boolean dryRun) implements Command<PurgeAnalyticsResult> {

  public static PurgeAnalyticsCommand scheduled() {
    return new PurgeAnalyticsCommand(false);
  }
}
