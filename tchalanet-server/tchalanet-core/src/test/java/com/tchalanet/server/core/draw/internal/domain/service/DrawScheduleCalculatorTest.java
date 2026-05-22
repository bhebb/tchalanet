package com.tchalanet.server.core.draw.internal.domain.service;

import com.tchalanet.server.core.draw.internal.domain.model.DrawScheduleSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DrawScheduleCalculator")
class DrawScheduleCalculatorTest {

    private static final ZoneId HAITI = ZoneId.of("America/Port-au-Prince");
    private static final LocalDate DRAW_DATE = LocalDate.of(2026, 5, 20);
    private static final LocalTime DRAW_TIME = LocalTime.of(14, 0); // 14:00 local
    private static final Duration CUTOFF_5_MIN = Duration.ofMinutes(5);

    private final DrawScheduleCalculator calculator = new DrawScheduleCalculator();

    @Nested
    @DisplayName("compute() — happy path")
    class HappyPath {

        @Test
        @DisplayName("scheduledAt is the instant corresponding to drawDate+drawTime in channelZone")
        void scheduledAtMatchesZonedDateTime() {
            DrawScheduleSnapshot snap = calculator.compute(DRAW_DATE, DRAW_TIME, HAITI, CUTOFF_5_MIN);

            // 2026-05-20T14:00 Haiti (UTC-4) = 2026-05-20T18:00:00Z
            Instant expected = Instant.parse("2026-05-20T18:00:00Z");
            assertThat(snap.scheduledAt()).isEqualTo(expected);
        }

        @Test
        @DisplayName("cutoffAt is exactly cutoffBeforeDraw before scheduledAt")
        void cutoffAtIsBeforeScheduledAt() {
            DrawScheduleSnapshot snap = calculator.compute(DRAW_DATE, DRAW_TIME, HAITI, CUTOFF_5_MIN);

            assertThat(snap.cutoffAt()).isEqualTo(snap.scheduledAt().minus(CUTOFF_5_MIN));
            assertThat(snap.cutoffAt()).isBefore(snap.scheduledAt());
        }

        @Test
        @DisplayName("snapshot preserves all input fields")
        void snapshotPreservesFields() {
            DrawScheduleSnapshot snap = calculator.compute(DRAW_DATE, DRAW_TIME, HAITI, CUTOFF_5_MIN);

            assertThat(snap.drawDate()).isEqualTo(DRAW_DATE);
            assertThat(snap.drawTime()).isEqualTo(DRAW_TIME);
            assertThat(snap.zoneId()).isEqualTo(HAITI);
        }

        @Test
        @DisplayName("DST-safe: UTC channel gives no offset")
        void utcChannelNoOffset() {
            ZoneId utc = ZoneId.of("UTC");
            DrawScheduleSnapshot snap = calculator.compute(
                LocalDate.of(2026, 1, 15),
                LocalTime.of(22, 30),
                utc,
                Duration.ofMinutes(10));

            assertThat(snap.scheduledAt()).isEqualTo(Instant.parse("2026-01-15T22:30:00Z"));
            assertThat(snap.cutoffAt()).isEqualTo(Instant.parse("2026-01-15T22:20:00Z"));
        }

        @Test
        @DisplayName("large cutoff (1 hour) is computed correctly")
        void largeCutoff() {
            DrawScheduleSnapshot snap = calculator.compute(
                DRAW_DATE, DRAW_TIME, HAITI, Duration.ofHours(1));

            assertThat(snap.cutoffAt()).isEqualTo(snap.scheduledAt().minusSeconds(3600));
        }
    }

    @Nested
    @DisplayName("compute() — validation errors")
    class ValidationErrors {

        @Test
        @DisplayName("null drawDate throws")
        void nullDrawDate() {
            assertThatThrownBy(() -> calculator.compute(null, DRAW_TIME, HAITI, CUTOFF_5_MIN))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("drawDate");
        }

        @Test
        @DisplayName("null drawTime throws")
        void nullDrawTime() {
            assertThatThrownBy(() -> calculator.compute(DRAW_DATE, null, HAITI, CUTOFF_5_MIN))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("drawTime");
        }

        @Test
        @DisplayName("null zoneId throws")
        void nullZoneId() {
            assertThatThrownBy(() -> calculator.compute(DRAW_DATE, DRAW_TIME, null, CUTOFF_5_MIN))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("zoneId");
        }

        @Test
        @DisplayName("null cutoffBeforeDraw throws")
        void nullCutoff() {
            assertThatThrownBy(() -> calculator.compute(DRAW_DATE, DRAW_TIME, HAITI, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("cutoffBeforeDraw");
        }

        @Test
        @DisplayName("zero cutoffBeforeDraw throws — cutoff must be strictly before draw")
        void zeroCutoff() {
            assertThatThrownBy(() -> calculator.compute(DRAW_DATE, DRAW_TIME, HAITI, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cutoffBeforeDraw must be > 0");
        }

        @Test
        @DisplayName("negative cutoffBeforeDraw throws")
        void negativeCutoff() {
            assertThatThrownBy(
                () -> calculator.compute(DRAW_DATE, DRAW_TIME, HAITI, Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cutoffBeforeDraw must not be negative");
        }
    }
}

