package com.tchalanet.server.core.analytics.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.analytics.api.command.PurgeAnalyticsCommand;
import com.tchalanet.server.core.analytics.api.model.PurgeAnalyticsResult;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDailyRepository;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDrawRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles {@link PurgeAnalyticsCommand}.
 *
 * <p>Retention windows (configurable, defaults per proposal):
 * <ul>
 *   <li>{@code analytics_daily} — 24 months</li>
 *   <li>{@code analytics_draw}  — 24 months</li>
 * </ul>
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class PurgeAnalyticsCommandHandler
    implements CommandHandler<PurgeAnalyticsCommand, PurgeAnalyticsResult> {

  private final AnalyticsDailyRepository dailyRepo;
  private final AnalyticsDrawRepository  drawRepo;

  @Value("${tchalanet.analytics.retention.daily-months:24}")
  private int dailyRetentionMonths;

  @Value("${tchalanet.analytics.retention.draw-months:24}")
  private int drawRetentionMonths;

  @Override
  @Transactional
  public PurgeAnalyticsResult handle(PurgeAnalyticsCommand cmd) {
    LocalDate dailyCutoff = LocalDate.now().minusMonths(dailyRetentionMonths);
    LocalDate drawCutoff  = LocalDate.now().minusMonths(drawRetentionMonths);

    if (cmd.dryRun()) {
      long dailyCount = dailyRepo.countOlderThan(dailyCutoff);
      long drawCount  = drawRepo.countOlderThan(drawCutoff);
      log.info("analytics purge dry-run: daily={} draw={}", dailyCount, drawCount);
      return new PurgeAnalyticsResult(dailyCount, drawCount, true);
    }

    int dailyDeleted = dailyRepo.deleteOlderThan(dailyCutoff);
    int drawDeleted  = drawRepo.deleteOlderThan(drawCutoff);
    log.info("analytics purge: daily={} draw={} deleted", dailyDeleted, drawDeleted);
    return new PurgeAnalyticsResult(dailyDeleted, drawDeleted, false);
  }
}
