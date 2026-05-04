package com.tchalanet.server.core.drawresult.infra.scheduler;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.common.batch.gate.BatchGate;
import com.tchalanet.server.common.batch.key.BatchJobKeys;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.time.OccurredAtResolver;
import com.tchalanet.server.core.draw.infra.config.DrawProperties;
import com.tchalanet.server.core.draw.infra.config.DrawSchedulerWindows;
import com.tchalanet.server.core.drawresult.application.command.model.FetchExternalResultsWindowCommand;
import com.tchalanet.server.core.drawresult.infra.config.DrawResultsProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalResultsFetchTickScheduler {

    private final CommandBus commandBus;
    private final BatchGate gate;
    private final ResultSlotCatalog resultSlotCatalog;
    private final DrawProperties drawProps;
    private final DrawResultsProperties resultsProps;
    private final DrawSchedulerWindows windows;
    private final Clock clock;

    private final Map<String, Instant> lastRunBySlot = new ConcurrentHashMap<>();

    @Scheduled(cron = "${tch.draw.results.scheduler.cron:0 */5 * * * *}", zone = "UTC")
    @SchedulerLock(name = "draw_results_fetch_tick", lockAtMostFor = "PT4M", lockAtLeastFor = "PT30S")
    public void tickFetch() {
        log.info("draw-result.fetch.tick fired");

        if (!drawProps.getScheduler().isActive()) return;
        if (!resultsProps.isActive()) return;
        if (!resultsProps.getScheduler().isActive()) return;

        if (!gate.enabled(BatchJobKeys.RESULTS_EXTERNAL_FETCH, null)) {
            log.info("draw-results.fetch.tick: gate=OFF");
            return;
        }

        var now = clock.instant();
        var localNow = now.atZone(drawProps.getScheduler().getWindows().getTimezone()).toLocalTime();

        if (!windows.isInFetchResultsWindow(localNow)) {
            log.debug("draw-results.fetch.tick: outside fetch window");
            return;
        }

        var dueSlots = resultSlotCatalog.listActive().stream()
            .filter(s -> s.drawTime() != null && s.timezone() != null)
            .filter(s -> isDue(s, now))
            .limit(resultsProps.getLimits().getMaxSlotsPerTick())
            .toList();

        if (dueSlots.isEmpty()) {
            log.info("draw-results.fetch.tick: dueSlots=0");
            return;
        }

        for (ResultSlotView slot : dueSlots) {
            try {
                var baseDate = now.atZone(slot.timezone()).toLocalDate();

                commandBus.send(new FetchExternalResultsWindowCommand(
                    null,
                    baseDate,
                    resultsProps.getDefaults().getManualDaysBack(),
                    List.of(slot.slotKey()),
                    false,
                    false,
                    resultsProps.getDefaults().getManualMaxSlots(),
                    null,
                    false
                ));

                markCooldown(slot.slotKey(), now);

            } catch (Exception e) {
                log.warn("draw-results.fetch.tick: slot={} failed err={}", slot.slotKey(), e.toString(), e);
            }
        }
    }

    private boolean isDue(ResultSlotView slot, Instant now) {
        var scheduler = resultsProps.getScheduler();

        var minAfter = Duration.ofMinutes(Math.max(0, scheduler.getMinMinutesAfterDraw()));
        var maxAfter = Duration.ofMinutes(Math.max(0, scheduler.getMaxMinutesAfterDraw()));

        var today = now.atZone(slot.timezone()).toLocalDate();

        return isDueForDate(slot, now, today, minAfter, maxAfter)
            || isDueForDate(slot, now, today.minusDays(1), minAfter, maxAfter);
    }

    private boolean isDueForDate(
        ResultSlotView slot,
        Instant now,
        LocalDate date,
        Duration minAfter,
        Duration maxAfter
    ) {
        var occurredAt = OccurredAtResolver.resolveOrThrow(
            null,
            date,
            slot.drawTime(),
            slot.timezone()
        );

        var age = Duration.between(occurredAt, now);

        if (age.isNegative()) return false;
        if (age.compareTo(minAfter) < 0) return false;
        if (age.compareTo(maxAfter) > 0) return false;

        return cooldownOk(slot.slotKey(), now);
    }

    private boolean cooldownOk(String slotKey, Instant now) {
        var cooldown = Duration.ofMinutes(
            Math.max(0, resultsProps.getScheduler().getCooldownMinutes())
        );

        var last = lastRunBySlot.get(slotKey);
        return last == null || Duration.between(last, now).compareTo(cooldown) >= 0;
    }

    private void markCooldown(String slotKey, Instant now) {
        lastRunBySlot.put(slotKey, now);
    }
}
