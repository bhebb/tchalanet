package com.tchalanet.server.core.drawresult.infra.scheduler;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.common.batch.gate.BatchGate;
import com.tchalanet.server.common.batch.key.BatchJobKeys;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.config.draw.DrawResultsCommonProperties;
import com.tchalanet.server.common.time.OccurredAtResolver;
import com.tchalanet.server.core.drawresult.application.command.model.FetchExternalResultsWindowCommand;
import com.tchalanet.server.core.drawresult.infra.config.DrawResultsProperties;
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
    private final DrawResultsProperties props; // fetch-specific properties
    private final DrawResultsCommonProperties commonProps; // common properties
    private final Clock clock;

    private final Map<String, Instant> lastRunBySlot = new ConcurrentHashMap<>();

    @Scheduled(cron = "${tch.draw.results.shared.scheduler.tick_cron:0 */5 * * * *}") // Use common props for cron
    @SchedulerLock(name = "draw_results_fetch_tick", lockAtMostFor = "PT4M", lockAtLeastFor = "PT30S")
    public void tickFetch() {
        if (!enabled()) return;

        var now = Instant.now(clock);
        var dueSlots = resultSlotCatalog.listActive().stream()
            .filter(s -> s.drawTime() != null && s.timezone() != null)
            .filter(s -> isDue(s, now))
            .limit(commonProps.getLimits().getHardMaxSlots()) // Use common props
            .toList();

        if (dueSlots.isEmpty()) {
            log.debug("draw-results.fetch.tick: dueSlots=0");
            return;
        }
        for (ResultSlotView slot : dueSlots) {
            var baseDate = LocalDate.now(clock.withZone(slot.timezone()));

            log.info("draw-results.fetch.tick: action=FETCH slotKey={} baseDate={} tz={}",
                slot.slotKey(), baseDate, slot.timezone());

            commandBus.send(new FetchExternalResultsWindowCommand(
                null, // tenantId is null for global fetch
                baseDate,
                0, // daysBack
                List.of(slot.slotKey()),
                false, // force
                false, // dryRun
                commonProps.getLimits().getHardMaxSlots(), // maxSlots from common props
                null // reason is null for scheduler-triggered command
            ));
        }
    }

    private boolean enabled() {
        if (!props.isActive() || !commonProps.getScheduler().isActive()) {
            log.debug("draw-results.fetch.tick: active=OFF");
            return false;
        }
        if (!gate.enabled(BatchJobKeys.RESULTS_EXTERNAL_FETCH, null)) {
            log.debug("draw-results.fetch.tick: gate=OFF");
            return false;
        }
        return true;
    }

    private boolean isDue(ResultSlotView slot, Instant now) {
        var dueCfg = commonProps.getScheduler().getDue(); // Use common props

        Duration minAfter = Duration.ofMinutes(Math.max(0, dueCfg.getMinMinutesAfterDraw()));
        Duration maxAfter = Duration.ofMinutes(Math.max(0, dueCfg.getMaxMinutesAfterDraw()));

        Instant drawInstant = OccurredAtResolver.resolve(
            null, LocalDate.now(clock.withZone(slot.timezone())), slot.drawTime(), slot.timezone(), clock);

        Duration age = Duration.between(drawInstant, now);

        if (age.isNegative()) return false;
        if (age.compareTo(minAfter) < 0) return false;
        if (age.compareTo(maxAfter) > 0) return false;

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
