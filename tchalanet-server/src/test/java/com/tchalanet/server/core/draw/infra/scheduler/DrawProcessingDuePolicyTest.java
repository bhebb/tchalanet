package com.tchalanet.server.core.draw.infra.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.core.draw.infra.config.DrawProperties;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DrawProcessingDuePolicyTest {

    private static final LocalDate DRAW_DATE = LocalDate.of(2026, 5, 6);
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    private final DrawProcessingDuePolicy policy = new DrawProcessingDuePolicy();

    @Test
    void isDueStartsAtConfiguredDelayAfterDrawTime() {
        var cfg = fetchConfig();
        var slot = slot();

        assertThat(policy.isDue(
            "fetch",
            slot,
            DRAW_DATE,
            instant("2026-05-06T18:34:59Z"),
            cfg)).isFalse();

        assertThat(policy.isDue(
            "fetch",
            slot,
            DRAW_DATE,
            instant("2026-05-06T18:35:00Z"),
            cfg)).isTrue();
    }

    @Test
    void isDueHonorsRetryIntervalPerStepSlotAndDate() {
        var cfg = fetchConfig();
        var slot = slot();
        var firstRun = instant("2026-05-06T18:35:00Z");

        assertThat(policy.isDue("fetch", slot, DRAW_DATE, firstRun, cfg)).isTrue();
        policy.markRun("fetch", slot.slotKey(), DRAW_DATE, firstRun);

        assertThat(policy.isDue(
            "fetch",
            slot,
            DRAW_DATE,
            instant("2026-05-06T18:44:59Z"),
            cfg)).isFalse();

        assertThat(policy.isDue(
            "fetch",
            slot,
            DRAW_DATE,
            instant("2026-05-06T18:45:00Z"),
            cfg)).isTrue();
    }

    @Test
    void isDueStopsAfterConfiguredWindow() {
        var cfg = fetchConfig();
        var slot = slot();

        assertThat(policy.isDue(
            "fetch",
            slot,
            DRAW_DATE,
            instant("2026-05-06T22:30:00Z"),
            cfg)).isTrue();

        assertThat(policy.isDue(
            "fetch",
            slot,
            DRAW_DATE,
            instant("2026-05-06T22:30:01Z"),
            cfg)).isFalse();
    }

    private static DrawProperties.Fetch fetchConfig() {
        var cfg = new DrawProperties.Fetch();
        cfg.setStartMinutesAfterDraw(5);
        cfg.setRetryEveryMinutes(10);
        cfg.setStopMinutesAfterDraw(240);
        return cfg;
    }

    private static ResultSlotView slot() {
        return new ResultSlotView(
            ResultSlotId.of(UUID.randomUUID()),
            "NY_MID",
            "US_LOTTERY",
            NEW_YORK,
            LocalTime.of(14, 30),
            "MON,TUE,WED,THU,FRI,SAT,SUN",
            true,
            null,
            null,
            "result_slot.ny_mid");
    }

    private static Instant instant(String value) {
        return Instant.parse(value);
    }
}
