package com.tchalanet.server.core.analytics.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.analytics.api.command.PurgeAnalyticsCommand;
import com.tchalanet.server.core.analytics.api.model.PurgeAnalyticsResult;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDailyRepository;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsDrawRepository;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsSelectionRepository;
import com.tchalanet.server.core.analytics.internal.infra.persistence.AnalyticsSellerTerminalDrawRepository;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

/**
 * Handles {@link PurgeAnalyticsCommand}.
 *
 * <p>Retention windows (configurable, defaults per proposal):
 *
 * <ul>
 *   <li>all four analytics projections — 24 months by default
 * </ul>
 */
@UseCase
@Slf4j
public class PurgeAnalyticsCommandHandler
    implements CommandHandler<PurgeAnalyticsCommand, PurgeAnalyticsResult> {

  private final AnalyticsDailyRepository dailyRepo;
  private final AnalyticsDrawRepository drawRepo;
  private final AnalyticsSelectionRepository selectionRepo;
  private final AnalyticsSellerTerminalDrawRepository sellerTerminalDrawRepo;
  private final int dailyRetentionMonths;
  private final int drawRetentionMonths;
  private final int selectionRetentionMonths;
  private final int sellerTerminalDrawRetentionMonths;

  public PurgeAnalyticsCommandHandler(
      AnalyticsDailyRepository dailyRepo,
      AnalyticsDrawRepository drawRepo,
      AnalyticsSelectionRepository selectionRepo,
      AnalyticsSellerTerminalDrawRepository sellerTerminalDrawRepo,
      @Value("${tchalanet.analytics.retention.daily-months:24}") int dailyRetentionMonths,
      @Value("${tchalanet.analytics.retention.draw-months:24}") int drawRetentionMonths,
      @Value("${tchalanet.analytics.retention.selection-months:24}") int selectionRetentionMonths,
      @Value("${tchalanet.analytics.retention.seller-terminal-draw-months:24}")
          int sellerTerminalDrawRetentionMonths) {
    this.dailyRepo = dailyRepo;
    this.drawRepo = drawRepo;
    this.selectionRepo = selectionRepo;
    this.sellerTerminalDrawRepo = sellerTerminalDrawRepo;
    this.dailyRetentionMonths = safeRetention(dailyRetentionMonths);
    this.drawRetentionMonths = safeRetention(drawRetentionMonths);
    this.selectionRetentionMonths = safeRetention(selectionRetentionMonths);
    this.sellerTerminalDrawRetentionMonths = safeRetention(sellerTerminalDrawRetentionMonths);
  }

  @Override
  @TchTx
  public PurgeAnalyticsResult handle(PurgeAnalyticsCommand cmd) {
    LocalDate dailyCutoff = LocalDate.now().minusMonths(dailyRetentionMonths);
    LocalDate drawCutoff = LocalDate.now().minusMonths(drawRetentionMonths);
    LocalDate selectionCutoff = LocalDate.now().minusMonths(selectionRetentionMonths);
    LocalDate sellerTerminalDrawCutoff =
        LocalDate.now().minusMonths(sellerTerminalDrawRetentionMonths);

    if (cmd.dryRun()) {
      long dailyCount = dailyRepo.countOlderThan(dailyCutoff);
      long drawCount = drawRepo.countOlderThan(drawCutoff);
      long selectionCount = selectionRepo.countOlderThan(selectionCutoff);
      long sellerTerminalDrawCount =
          sellerTerminalDrawRepo.countOlderThan(sellerTerminalDrawCutoff);
      log.info(
          "analytics purge dry-run: daily={} draw={} selection={} sellerTerminalDraw={}",
          dailyCount,
          drawCount,
          selectionCount,
          sellerTerminalDrawCount);
      return new PurgeAnalyticsResult(
          dailyCount, drawCount, selectionCount, sellerTerminalDrawCount, true);
    }

    int dailyDeleted = dailyRepo.deleteOlderThan(dailyCutoff);
    int drawDeleted = drawRepo.deleteOlderThan(drawCutoff);
    int selectionDeleted = selectionRepo.deleteOlderThan(selectionCutoff);
    int sellerTerminalDrawDeleted =
        sellerTerminalDrawRepo.deleteOlderThan(sellerTerminalDrawCutoff);
    log.info(
        "analytics purge: daily={} draw={} selection={} sellerTerminalDraw={} deleted",
        dailyDeleted,
        drawDeleted,
        selectionDeleted,
        sellerTerminalDrawDeleted);
    return new PurgeAnalyticsResult(
        dailyDeleted, drawDeleted, selectionDeleted, sellerTerminalDrawDeleted, false);
  }

  private static int safeRetention(int months) {
    // A malformed value must never turn a retention purge into an all-rows delete.
    return Math.max(1, months);
  }
}
