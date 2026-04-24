package com.tchalanet.server.core.drawresult.infra.scheduler;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.common.batch.gate.BatchGate;
import com.tchalanet.server.common.batch.key.BatchJobKeys;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.core.drawresult.application.command.model.FetchExternalResultsWindowCommand;
import com.tchalanet.server.core.drawresult.infra.config.DrawResultsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final DrawResultsProperties props;
    private final Clock clock;

    private final Map<String, Instant> lastRunBySlot = new ConcurrentHashMap<>();

    @Scheduled(cron = "${tch.draw.results.scheduler.fetch_cron:0 */5 * * * *}")
    public void tickFetch() {
        if (!enabled()) return;

        var now = Instant.now(clock);
        var dueSlots = resultSlotCatalog.listActive().stream()
            .filter(s -> s.drawTime() != null && s.timezone() != null)
            .filter(s -> isDue(s, now))
            .limit(hardMaxSlots())
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
                null,
                baseDate,
                0,
                List.of(slot.slotKey()),
                false,
                false,
                50
            ));
        }
    }

    private boolean enabled() {
        if (!props.isActive() || !props.getScheduler().isActive()) {
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
        var dueCfg = props.getScheduler().getDue();

        Duration minAfter = Duration.ofMinutes(Math.max(0, dueCfg.getMinMinutesAfterDraw()));
        Duration maxAfter = Duration.ofMinutes(Math.max(0, dueCfg.getMaxMinutesAfterDraw()));

        Instant drawInstant = drawInstantToday(slot, now);
        Duration age = Duration.between(drawInstant, now);

        if (age.isNegative()) return false;
        if (age.compareTo(minAfter) < 0) return false;
        if (age.compareTo(maxAfter) > 0) return false;

        return cooldownOk(slot.slotKey(), now);
    }

    private Instant drawInstantToday(ResultSlotView slot, Instant now) {
        ZonedDateTime nowZ = ZonedDateTime.ofInstant(now, slot.timezone());
        return nowZ.toLocalDate()
            .atTime(slot.drawTime())
            .atZone(slot.timezone())
            .toInstant();
    }

    private boolean cooldownOk(String slotKey, Instant now) {
        Duration cooldown = Duration.ofMinutes(Math.max(0, props.getScheduler().getCooldownMinutes()));
        Instant last = lastRunBySlot.get(slotKey);
        if (last != null && Duration.between(last, now).compareTo(cooldown) < 0) return false;

        lastRunBySlot.put(slotKey, now);
        return true;
    }

    private long hardMaxSlots() {
        return props.getLimits() == null ? 200 : props.getLimits().getHardMaxSlots();
    }
}

