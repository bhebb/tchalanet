package com.tchalanet.server.core.analytics.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.analytics.api.command.RecomputeAnalyticsDailyCommand;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDailyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles {@link RecomputeAnalyticsDailyCommand}.
 *
 * <p><b>V1 stub implementation</b>: deletes existing rows older than {@code from - 1 day}
 * and logs the operation. Full recompute from source-of-truth tables (sales_ticket,
 * payout, session) is implemented in {@code migrate-feature-stats-to-core-analytics}.
 *
 * <p>The stub is safe to deploy: it clears stale rows and event-driven projectors
 * will rebuild from the next activity.
 *
 * TODO migrate-feature-stats-to-core-analytics: replace stub with real
 * source-of-truth aggregation queries.
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class RecomputeAnalyticsDailyCommandHandler
    implements CommandHandler<RecomputeAnalyticsDailyCommand, Void> {

  private final AnalyticsDailyRepository repo;

  @Override
  @Transactional
  public Void handle(RecomputeAnalyticsDailyCommand cmd) {
    log.info("analytics recompute: tenant={} from={} to={}",
        cmd.tenantId() != null ? cmd.tenantId().value() : "ALL",
        cmd.from(), cmd.to());

    // V1 stub: purge the window; projectors rebuild on next activity.
    int deleted = repo.deleteOlderThan(cmd.from().minusDays(1));
    log.info("analytics recompute: {} rows purged for window {}-{}", deleted, cmd.from(), cmd.to());

    return null;
  }
}
