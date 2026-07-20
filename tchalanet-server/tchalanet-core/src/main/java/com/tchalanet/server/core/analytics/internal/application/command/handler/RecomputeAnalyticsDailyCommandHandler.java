package com.tchalanet.server.core.analytics.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.analytics.api.command.RecomputeAnalyticsDailyCommand;

/**
 * Handles {@link RecomputeAnalyticsDailyCommand}.
 *
 * <p>This legacy command used to delete rows outside its tenant/date scope. It is deliberately
 * disabled until callers migrate to {@code ReconcileAnalyticsCommand}, which validates after a
 * targeted rebuild.
 */
@UseCase
public class RecomputeAnalyticsDailyCommandHandler
    implements CommandHandler<RecomputeAnalyticsDailyCommand, Void> {

  @Override
  public Void handle(RecomputeAnalyticsDailyCommand cmd) {
    throw new UnsupportedOperationException(
        "analytics.recompute.legacy_disabled: use ReconcileAnalyticsCommand");
  }
}
