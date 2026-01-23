package com.tchalanet.server.core.drawresult.infra.batch.config;

import com.tchalanet.server.common.batch.gate.BatchGate;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.core.drawresult.application.command.model.RefreshExternalResultsWindowCommand;
import com.tchalanet.server.core.drawresult.infra.config.DrawResultsProperties;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
  private final ResultSlotCatalog resultSlotReader;
  private final DrawResultsProperties props;
  private final Clock clock;

  // in-memory cooldown: slotKey -> lastRefreshInstant
  private final Map<String, Instant> lastRun = new ConcurrentHashMap<>();

  @Scheduled(cron = "${tch.draw.results.scheduler.tick_cron:0 */5 * * * *}", zone = "UTC")
  public void tick() {
    if (!props.isActive() || !props.getScheduler().isActive()) {
      log.debug("draw-results.scheduler: active=OFF");
      return;
    }
    if (!gate.enabled("results.refresh.enabled", null)) {
      log.debug("draw-results.scheduler: gate=OFF(refresh)");
      return;
    }

    var dueKeys = resolveDueSlotKeys();
    if (dueKeys.isEmpty()) {
      log.debug("draw-results.scheduler: dueSlots=0");
      return;
    }

    log.info("draw-results.scheduler: action=REFRESH dueSlots={}", dueKeys);

    // one command per slot = simpler & isolates failures
    for (var slotKey : dueKeys) {
      var slotOpt = resultSlotReader.findBySlotKey(slotKey);
      if (slotOpt.isEmpty()) continue;

      var slot = slotOpt.get();
      var baseDate = LocalDate.now(slot.timezone());

      commandBus.send(
          new RefreshExternalResultsWindowCommand(
              /* tenantId */ null, // refresh = platform/global orchestration
              baseDate,
              /* daysBack */ 0,
              List.of(slot.slotKey()),
              /* force */ false,
              /* dryRun */ false,
              /* maxSlots */ 50));
    }
  }

  private List<String> resolveDueSlotKeys() {
    Instant now = Instant.now(clock);

    int minAfter = props.getScheduler().getDue().getMinMinutesAfterDraw();
    int maxAfter = props.getScheduler().getDue().getMaxMinutesAfterDraw();
    Duration minDelay = Duration.ofMinutes(Math.max(0, minAfter));
    Duration maxDelay = Duration.ofMinutes(Math.max(0, maxAfter));

    Duration cooldown = Duration.ofMinutes(Math.max(0, props.getScheduler().getCooldownMinutes()));

    List<String> out = new ArrayList<>();
    for (var slot : resultSlotReader.listActive()) {
      if (slot.drawTime() == null || slot.timezone() == null) continue;

      var z = slot.timezone();
      var nowZ = ZonedDateTime.ofInstant(now, z);

      // draw instant "today at draw_time" in slot TZ
      var drawZ = nowZ.toLocalDate().atTime(slot.drawTime()).atZone(z);
      var age = Duration.between(drawZ.toInstant(), now);

      // must be after draw and within window
      if (age.isNegative()) continue;
      if (age.compareTo(minDelay) < 0) continue;
      if (age.compareTo(maxDelay) > 0) continue;

      // cooldown
      Instant last = lastRun.get(slot.slotKey());
      if (last != null && Duration.between(last, now).compareTo(cooldown) < 0) continue;

      lastRun.put(slot.slotKey(), now);
      out.add(slot.slotKey());
    }
    return out;
  }
}
