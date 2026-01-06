package com.tchalanet.server.core.draw.infra.batch.config;

import com.tchalanet.server.common.batch.BatchGate;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.time.DefaultTimeZone;
import com.tchalanet.server.core.draw.application.command.model.ApplyExternalResultsForDateCommand;
import com.tchalanet.server.core.draw.application.command.model.FetchExternalResultsForDateCommand;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawResultsScheduler {

  private final CommandBus commandBus;
  private final BatchGate gate;

  @Scheduled(cron = "0 35,40,45,50,55 14 * * *", zone = "America/New_York")
  @Scheduled(cron = "0 0 15 * * *", zone = "America/New_York")
  public void nyMiddayRefresh() {
    refresh(
        "NY_MIDDAY",
        DefaultTimeZone.AMERICA_NEW_YORK,
        List.of("US_NY_NUM3_MID", "US_NY_NUM4_MID"),
        500,
        false,
        false);
  }

  @Scheduled(cron = "0 50,55,59 22 * * *", zone = "America/New_York")
  public void nyEveRefresh() {
    refresh(
        "NY_EVE",
        DefaultTimeZone.AMERICA_NEW_YORK,
        List.of("US_NY_NUM3_EVE", "US_NY_NUM4_EVE"),
        10500,
        false,
        false);
  }

  @Scheduled(cron = "0 35,40,45,50,55 13 * * *", zone = "America/New_York")
  @Scheduled(cron = "0 0 14 * * *", zone = "America/New_York")
  public void flMiddayRefresh() {
    refresh(
        "FL_MIDDAY",
        DefaultTimeZone.AMERICA_NEW_YORK,
        List.of("US_FL_PICK3_MID", "US_FL_PICK4_MID"),
        700,
        false,
        false);
  }

  @Scheduled(cron = "0 50,55 22 * * *", zone = "America/New_York")
  @Scheduled(cron = "0 0,5,10,15 23 * * *", zone = "America/New_York")
  public void flEveningRefresh() {
    refresh(
        "FL_EVENING",
        DefaultTimeZone.AMERICA_NEW_YORK,
        List.of("US_FL_PICK3_EVE", "US_FL_PICK4_EVE"),
        900,
        false,
        false);
  }

  @Scheduled(cron = "0 20,25,30,35,40,45 23 * * *", zone = "America/New_York")
  public void flLottoRefresh() {
    refresh(
        "FL_LOTTO", DefaultTimeZone.AMERICA_NEW_YORK, List.of("US_FL_LOTTO"), 300, false, false);
  }

  private void refresh(
      String slot,
      ZoneId zone,
      List<String> channelCodes,
      int maxDraws,
      boolean force,
      boolean dryRun) {

    var drawDate = LocalDate.now(zone);

    if (!gate.enabled("results.fetch.enabled", null)) {
      log.debug("draw-results.scheduler: slot={} gate=OFF(fetch)", slot);
      return;
    }

    try {
      log.info(
          "draw-results.scheduler: slot={} action=REFRESH step=FETCH date={} channels={} maxDraws={} force={} dryRun={}",
          slot,
          drawDate,
          channelCodes.size(),
          maxDraws,
          force,
          dryRun);

      commandBus.send(
          new FetchExternalResultsForDateCommand(
              null, drawDate, channelCodes, force, dryRun, maxDraws));

    } catch (Exception e) {
      log.warn(
          "draw-results.scheduler: slot={} action=REFRESH step=FETCH FAILED date={} err={}",
          slot,
          drawDate,
          e.toString(),
          e);
      return; // apply skipped
    }

    if (!gate.enabled("results.apply.enabled", null)) {
      log.info("draw-results.scheduler: slot={} gate=OFF(apply)", slot);
      return;
    }

    try {
      log.info(
          "draw-results.scheduler: slot={} action=REFRESH step=APPLY date={} channels={} maxDraws={} force={} dryRun={}",
          slot,
          drawDate,
          channelCodes.size(),
          maxDraws,
          force,
          dryRun);

      commandBus.send(
          new ApplyExternalResultsForDateCommand(
              null, drawDate, channelCodes, force, dryRun, maxDraws));

    } catch (Exception e) {
      log.warn(
          "draw-results.scheduler: slot={} action=REFRESH step=APPLY FAILED date={} err={}",
          slot,
          drawDate,
          e.toString(),
          e);
    }
  }
}
