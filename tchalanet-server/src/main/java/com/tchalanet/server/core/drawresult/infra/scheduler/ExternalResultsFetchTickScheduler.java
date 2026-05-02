package com.tchalanet.server.core.drawresult.infra.scheduler;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.common.batch.gate.BatchGate;
import com.tchalanet.server.common.batch.key.BatchJobKeys;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.config.draw.DrawResultsCommonProperties;
import com.tchalanet.server.common.time.OccurredAtResolver;
import com.tchalanet.server.core.drawresult.application.command.model.FetchExternalResultsWindowCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalResultsFetchTickScheduler {

    private final CommandBus commandBus;
    private final BatchGate gate;
    private final ResultSlotCatalog resultSlotCatalog;
    private final DrawResultsCommonProperties commonProps; // fetch-specific properties
    private final Clock clock;

    private final Map<String, Instant> lastRunBySlot = new ConcurrentHashMap<>();

    @Scheduled(cron = "${tch.draw.results.scheduler.tick_cron:0 */5 * * * *}", zone = "UTC")
    @SchedulerLock(name = "draw_results_fetch_tick", lockAtMostFor = "PT4M", lockAtLeastFor = "PT30S")
    public void tickFetch() {
        log.info("draw-result.fetch.tick fired");
        if (!enabled()) return;

        var now = clock.instant();

        var dueSlots = resultSlotCatalog.listActive().stream()
            .filter(s -> s.drawTime() != null && s.timezone() != null)
            .filter(s -> isDue(s, now))
            .limit(commonProps.getLimits().getHardMaxSlots())
            .toList();

        if (dueSlots.isEmpty()) {
            log.info("draw-results.fetch.tick: dueSlots=0");
            return;
        }

        for (ResultSlotView slot : dueSlots) {
            var baseDate = now.atZone(slot.timezone()).toLocalDate();

            commandBus.send(new FetchExternalResultsWindowCommand(
                null,
                baseDate,
                commonProps.getDefaults().getDaysBack(),
                List.of(slot.slotKey()),
                false,
                false,
                commonProps.getDefaults().getMaxSlots(),
                null,
                false
            ));
        }
    }

    private boolean enabled() {
        if (!commonProps.isActive() || !commonProps.getScheduler().isActive()) {
            log.info("draw-results.fetch.tick: active=OFF");
            return false;
        }
        if (!gate.enabled(BatchJobKeys.RESULTS_EXTERNAL_FETCH, null)) {
            log.info("draw-results.fetch.tick: gate=OFF");
            return false;
        }
        return true;
    }

    private boolean isDue(ResultSlotView slot, Instant now) {
        var due = commonProps.getScheduler().getDue();

        var minAfter = Duration.ofMinutes(Math.max(0, due.getMinMinutesAfterDraw()));
        var maxAfter = Duration.ofMinutes(Math.max(0, due.getMaxMinutesAfterDraw()));
        var lookback = Duration.ofMinutes(Math.max(0, due.getLookbackMinutes()));

        var zone = slot.timezone();
        var today = now.atZone(zone).toLocalDate();

        return isDueForDate(slot, now, today, minAfter, maxAfter, lookback)
            || isDueForDate(slot, now, today.minusDays(1), minAfter, maxAfter, lookback);
    }

    private boolean isDueForDate(
        ResultSlotView slot,
        Instant now,
        LocalDate date,
        Duration minAfter,
        Duration maxAfter,
        Duration lookback) {

        Instant drawInstant = OccurredAtResolver.resolve(
            null,
            date,
            slot.drawTime(),
            slot.timezone(),
            clock
        );

        var age = Duration.between(drawInstant, now);

        if (age.isNegative()) return false;
        if (age.compareTo(minAfter) < 0) return false;
        if (age.compareTo(maxAfter) > 0) return false;
        if (lookback.toMinutes() > 0 && age.compareTo(lookback) > 0) return false;

        return cooldownOk(slot.slotKey(), now);
    }

    private boolean cooldownOk(String slotKey, Instant now) {
        Duration cooldown = Duration.ofMinutes(Math.max(0, commonProps.getScheduler().getCooldownMinutes())); // Use common props
        Instant last = lastRunBySlot.get(slotKey);
        if (last != null && Duration.between(last, now).compareTo(cooldown) < 0) return false;

        lastRunBySlot.put(slotKey, now);
        return true;
    }
}
