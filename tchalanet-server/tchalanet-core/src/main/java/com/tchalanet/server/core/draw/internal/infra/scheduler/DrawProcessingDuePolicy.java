package com.tchalanet.server.core.draw.internal.infra.scheduler;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.common.time.OccurredAtResolver;
import com.tchalanet.server.core.draw.internal.infra.config.DrawProperties;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class DrawProcessingDuePolicy {

    private final Map<String, Instant> lastRunByFingerprint = new ConcurrentHashMap<>();

    public boolean isDue(
        String step,
        ResultSlotView slot,
        LocalDate drawDate,
        Instant now,
        DrawProperties.DueAfterDraw config
    ) {
        Objects.requireNonNull(step, "step is required");
        Objects.requireNonNull(slot, "slot is required");
        Objects.requireNonNull(drawDate, "drawDate is required");
        Objects.requireNonNull(now, "now is required");
        Objects.requireNonNull(config, "config is required");

        if (!config.isActive()) return false;
        if (slot.drawTime() == null || slot.timezone() == null) return false;

        var occurredAt = OccurredAtResolver.resolveOrThrow(
            null,
            drawDate,
            slot.drawTime(),
            slot.timezone());

        var age = Duration.between(occurredAt, now);
        if (age.isNegative()) return false;
        if (age.compareTo(Duration.ofMinutes(Math.max(0, config.getStartMinutesAfterDraw()))) < 0) {
            return false;
        }
        if (age.compareTo(Duration.ofMinutes(Math.max(0, config.getStopMinutesAfterDraw()))) > 0) {
            return false;
        }

        var lastRun = lastRunByFingerprint.get(fingerprint(step, slot.slotKey(), drawDate));
        return lastRun == null
            || Duration.between(lastRun, now)
            .compareTo(Duration.ofMinutes(Math.max(0, config.getRetryEveryMinutes()))) >= 0;
    }

    public void markRun(String step, String slotKey, LocalDate drawDate, Instant now) {
        Objects.requireNonNull(now, "now is required");
        lastRunByFingerprint.put(fingerprint(step, slotKey, drawDate), now);
    }

    private static String fingerprint(String step, String slotKey, LocalDate drawDate) {
        return normalize(step) + ":" + normalize(slotKey) + ":" + drawDate;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
