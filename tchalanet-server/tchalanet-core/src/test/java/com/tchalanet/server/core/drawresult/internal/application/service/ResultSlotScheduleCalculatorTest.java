package com.tchalanet.server.core.drawresult.internal.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ResultSlotScheduleCalculator")
class ResultSlotScheduleCalculatorTest {

    private static final String TZ = "America/Port-au-Prince";
    private static final LocalTime DRAW_TIME = LocalTime.of(13, 0);

    @Test
    @DisplayName("returns WAITING with positive countdown when before draw time")
    void waitingBeforeDrawTime() {
        var clock = Clock.fixed(
            Instant.parse("2026-07-13T16:00:00Z"), // 12:00 local (UTC-4)
            ZoneId.of(TZ)
        );
        var calc = new ResultSlotScheduleCalculator(clock);

        var result = calc.calculateNextResultTime(DRAW_TIME, TZ, true);

        assertThat(result.status()).isEqualTo("WAITING");
        assertThat(result.countdownSeconds()).isEqualTo(3600);
        assertThat(result.localTime()).isEqualTo(DRAW_TIME);
    }

    @Test
    @DisplayName("returns next day when after draw time")
    void nextDayAfterDrawTime() {
        var clock = Clock.fixed(
            Instant.parse("2026-07-13T18:00:00Z"), // 14:00 local
            ZoneId.of(TZ)
        );
        var calc = new ResultSlotScheduleCalculator(clock);

        var result = calc.calculateNextResultTime(DRAW_TIME, TZ, true);

        assertThat(result.status()).isEqualTo("WAITING");
        assertThat(result.countdownSeconds()).isGreaterThan(0);
        assertThat(result.localDate().toString()).isEqualTo("2026-07-14");
    }

    @Test
    @DisplayName("returns DISABLED with zero countdown when slot is inactive")
    void disabledSlot() {
        var clock = Clock.fixed(
            Instant.parse("2026-07-13T16:00:00Z"),
            ZoneId.of(TZ)
        );
        var calc = new ResultSlotScheduleCalculator(clock);

        var result = calc.calculateNextResultTime(DRAW_TIME, TZ, false);

        assertThat(result.status()).isEqualTo("DISABLED");
        assertThat(result.countdownSeconds()).isZero();
    }

    @Test
    @DisplayName("countdown is zero at exact draw time (transitions to next day)")
    void atExactDrawTime() {
        var clock = Clock.fixed(
            Instant.parse("2026-07-13T17:00:00Z"), // 13:00 local = draw time
            ZoneId.of(TZ)
        );
        var calc = new ResultSlotScheduleCalculator(clock);

        var result = calc.calculateNextResultTime(DRAW_TIME, TZ, true);

        assertThat(result.status()).isEqualTo("WAITING");
        assertThat(result.localDate().toString()).isEqualTo("2026-07-14");
    }
}
