package com.tchalanet.server.common.time;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TimeProvider")
class TimeProviderTest {

    private static final Instant FIXED = Instant.parse("2026-05-21T03:00:00Z");
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final ZoneId HAITI = ZoneId.of("America/Port-au-Prince");
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    // Fixed clock anchored at 2026-05-21T03:00:00Z (= 2026-05-20T23:00:00 Eastern)
    private final TimeProvider provider = new TimeProvider(Clock.fixed(FIXED, UTC));

    @Nested
    @DisplayName("now()")
    class NowTests {

        @Test
        @DisplayName("returns the underlying clock instant")
        void returnsClockInstant() {
            assertThat(provider.now()).isEqualTo(FIXED);
        }

        @Test
        @DisplayName("nowInstant() is an alias for now()")
        void nowInstantAlias() {
            assertThat(provider.nowInstant()).isEqualTo(provider.now());
        }
    }

    @Nested
    @DisplayName("today(ZoneId)")
    class TodayTests {

        @Test
        @DisplayName("UTC zone returns UTC date")
        void utcDate() {
            assertThat(provider.today(UTC)).isEqualTo(LocalDate.of(2026, 5, 21));
        }

        @Test
        @DisplayName("Eastern zone returns previous calendar day when past midnight UTC")
        void easternDateBeforeMidnight() {
            // 03:00 UTC = 23:00 Eastern (UTC-4, EDT), so it's still May 20
            assertThat(provider.today(NEW_YORK)).isEqualTo(LocalDate.of(2026, 5, 20));
        }

        @Test
        @DisplayName("Haiti zone returns same as Eastern (same UTC offset)")
        void haitiSameAsEastern() {
            assertThat(provider.today(HAITI)).isEqualTo(LocalDate.of(2026, 5, 20));
        }
    }

    @Nested
    @DisplayName("nowAt(ZoneId)")
    class NowAtTests {

        @Test
        @DisplayName("UTC zone returns date-time at the fixed instant")
        void utcDateTime() {
            ZonedDateTime result = provider.nowAt(UTC);
            assertThat(result.toInstant()).isEqualTo(FIXED);
            assertThat(result.getHour()).isEqualTo(3);
        }

        @Test
        @DisplayName("Eastern zone is 4 hours behind UTC")
        void easternDateTime() {
            ZonedDateTime result = provider.nowAt(NEW_YORK);
            assertThat(result.toInstant()).isEqualTo(FIXED);
            assertThat(result.getHour()).isEqualTo(23);
        }
    }

    @Nested
    @DisplayName("clock()")
    class ClockTests {

        @Test
        @DisplayName("exposes the underlying clock")
        void exposesUnderlyingClock() {
            assertThat(provider.clock().instant()).isEqualTo(FIXED);
        }
    }
}

