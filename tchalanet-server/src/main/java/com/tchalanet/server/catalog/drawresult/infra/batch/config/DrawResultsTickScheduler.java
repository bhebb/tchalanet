package com.tchalanet.server.catalog.drawresult.infra.batch.config;

import com.tchalanet.server.common.batch.gate.BatchGate;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.catalog.drawresult.application.command.model.RefreshExternalResultsWindowCommand;
import com.tchalanet.server.catalog.drawresult.infra.config.DrawResultsProperties;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawResultsTickScheduler {

  private final CommandBus commandBus;
  private final BatchGate gate;
  private final ResultSlotCatalog resultSlotReader;
  private final DrawResultsProperties props;
  private final Clock clock;

  /**
   * Cooldown anti-spam par slotKey (MVP mono-instance). En multi-instance, on remplacera par Redis
   * (SETNX TTL) ou table dédiée.
   */
  private final Map<String, Instant> lastRunBySlot = new ConcurrentHashMap<>();

  @Scheduled(cron = "${tch.draw.results.scheduler.tick_cron:0 */5 * * * *}", zone = "UTC")
  public void tick() {
    if (!props.isActive() || !props.getScheduler().isActive()) {
      log.debug("draw-results.tick: active=OFF");
      return;
    }

    if (!gate.enabled("results.refresh.enabled", null)) {
      log.debug("draw-results.tick: gate=OFF");
      return;
    }

    var due = resolveDueSlots();
    if (due.isEmpty()) {
      log.debug("draw-results.tick: dueSlots=0");
      return;
    }

    // MVP: on envoie un refresh par slot pour isoler erreurs + baseDate correcte par timezone
    for (var slotKey : due) {
      var slotOpt = resultSlotReader.findBySlotKey(slotKey);
      if (slotOpt.isEmpty()) continue;

      var slot = slotOpt.get();
      var baseDate = LocalDate.now(slot.timezone());

      log.info(
          "draw-results.tick: action=REFRESH slotKey={} baseDate={} tz={}",
          slot.slotKey(),
          baseDate,
          slot.timezone());

      commandBus.send(
          new RefreshExternalResultsWindowCommand(
              /* tenantId */ null,
              baseDate,
              /* daysBack */ 0,
              List.of(slot.slotKey()),
              /* force */ false,
              /* dryRun */ false,
              /* maxSlots */ 50));
    }
  }

  private List<String> resolveDueSlots() {
    var dueCfg = props.getScheduler().getDue();
    Duration minAfter = Duration.ofMinutes(Math.max(0, dueCfg.getMinMinutesAfterDraw()));
    Duration maxAfter = Duration.ofMinutes(Math.max(0, dueCfg.getMaxMinutesAfterDraw()));
    Duration cooldown = Duration.ofMinutes(Math.max(0, props.getScheduler().getCooldownMinutes()));

    Instant now = Instant.now(clock);

    List<String> out = new ArrayList<>();
    for (var slot : resultSlotReader.listActive()) {
      if (!slot.active()) continue;
      if (slot.drawTime() == null || slot.timezone() == null) continue;

      // cooldown check
      Instant last = lastRunBySlot.get(slot.slotKey());
      if (last != null && Duration.between(last, now).compareTo(cooldown) < 0) {
        continue;
      }

      var nowZ = ZonedDateTime.ofInstant(now, slot.timezone());
      var drawToday = nowZ.toLocalDate().atTime(slot.drawTime()).atZone(slot.timezone());

      Duration age = Duration.between(drawToday.toInstant(), now);

      // must be after draw and within window
      if (age.isNegative()) continue;
      if (age.compareTo(minAfter) < 0) continue;
      if (age.compareTo(maxAfter) > 0) continue;

      // mark cooldown now
      lastRunBySlot.put(slot.slotKey(), now);
      out.add(slot.slotKey());
    }

    // hard cap from properties (safety)
    int hardMax = props.getLimits() == null ? 200 : props.getLimits().getHardMaxSlots();
    if (out.size() > hardMax) return out.subList(0, hardMax);

    return out;
  }
}
