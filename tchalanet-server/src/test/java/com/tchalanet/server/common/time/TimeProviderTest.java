package com.tchalanet.server.common.time;

import com.tchalanet.server.common.context.TchRequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("TimeProvider")
class TimeProviderTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-21T15:30:00Z");
    private Clock fixedClock;
    private TimeProvider timeProvider;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(FIXED_INSTANT, ZoneId.of("UTC"));
        timeProvider = new TimeProvider(fixedClock);
    }

    @Nested
    @DisplayName("When using explicit ZoneId")
    class WhenUsingExplicitZoneId {

        @Test
        @DisplayName("should return current time in UTC when zone is UTC")
        void shouldReturnCurrentTimeInUtcWhenZoneIsUtc() {
            // given
            ZoneId zone = ZoneId.of("UTC");

            // when
            ZonedDateTime result = timeProvider.now(zone);

            // then
            assertThat(result.toInstant()).isEqualTo(FIXED_INSTANT);
            assertThat(result.getZone()).isEqualTo(zone);
            assertThat(result.getYear()).isEqualTo(2026);
            assertThat(result.getMonthValue()).isEqualTo(1);
            assertThat(result.getDayOfMonth()).isEqualTo(21);
            assertThat(result.getHour()).isEqualTo(15);
            assertThat(result.getMinute()).isEqualTo(30);
        }

        @Test
        @DisplayName("should return current time in New York when zone is America/New_York")
        void shouldReturnCurrentTimeInNewYorkWhenZoneIsAmericaNewYork() {
            // given
            ZoneId zone = ZoneId.of("America/New_York");

            // when
            ZonedDateTime result = timeProvider.now(zone);

            // then
            assertThat(result.toInstant()).isEqualTo(FIXED_INSTANT);
            assertThat(result.getZone()).isEqualTo(zone);
            // 15:30 UTC = 10:30 EST (winter time)
            assertThat(result.getHour()).isEqualTo(10);
            assertThat(result.getMinute()).isEqualTo(30);
        }

        @Test
        @DisplayName("should return current time in Paris when zone is Europe/Paris")
        void shouldReturnCurrentTimeInParisWhenZoneIsEuropeParis() {
            // given
            ZoneId zone = ZoneId.of("Europe/Paris");

            // when
            ZonedDateTime result = timeProvider.now(zone);

            // then
            assertThat(result.toInstant()).isEqualTo(FIXED_INSTANT);
            assertThat(result.getZone()).isEqualTo(zone);
            // 15:30 UTC = 16:30 CET (winter time)
            assertThat(result.getHour()).isEqualTo(16);
            assertThat(result.getMinute()).isEqualTo(30);
        }

        @Test
        @DisplayName("should return today in UTC when zone is UTC")
        void shouldReturnTodayInUtcWhenZoneIsUtc() {
            // given
            ZoneId zone = ZoneId.of("UTC");

            // when
            LocalDate result = timeProvider.today(zone);

            // then
            assertThat(result.getYear()).isEqualTo(2026);
            assertThat(result.getMonthValue()).isEqualTo(1);
            assertThat(result.getDayOfMonth()).isEqualTo(21);
        }

        @Test
        @DisplayName("should return correct date in Tokyo when day changes")
        void shouldReturnCorrectDateInTokyoWhenDayChanges() {
            // given
            // 15:30 UTC = 00:30 JST next day (Tokyo is UTC+9)
            ZoneId zone = ZoneId.of("Asia/Tokyo");

            // when
            LocalDate result = timeProvider.today(zone);

            // then
            // 15:30 UTC on 2026-01-21 = 00:30 on 2026-01-22 in Tokyo
            assertThat(result.getYear()).isEqualTo(2026);
            assertThat(result.getMonthValue()).isEqualTo(1);
            assertThat(result.getDayOfMonth()).isEqualTo(22);
        }
    }

    @Nested
    @DisplayName("When using TchRequestContext")
    class WhenUsingTchRequestContext {

        @Test
        @DisplayName("should return current time in context's effective zone")
        void shouldReturnCurrentTimeInContextEffectiveZone() {
            // given
            ZoneId contextZone = ZoneId.of("Europe/Paris");
            TchRequestContext ctx = mock(TchRequestContext.class);
            when(ctx.effectiveZoneId()).thenReturn(contextZone);

            // when
            ZonedDateTime result = timeProvider.now(ctx);

            // then
            assertThat(result.toInstant()).isEqualTo(FIXED_INSTANT);
            assertThat(result.getZone()).isEqualTo(contextZone);
            assertThat(result.getHour()).isEqualTo(16); // 15:30 UTC = 16:30 CET
        }

        @Test
        @DisplayName("should return today in context's effective zone")
        void shouldReturnTodayInContextEffectiveZone() {
            // given
            ZoneId contextZone = ZoneId.of("Asia/Tokyo");
            TchRequestContext ctx = mock(TchRequestContext.class);
            when(ctx.effectiveZoneId()).thenReturn(contextZone);

            // when
            LocalDate result = timeProvider.today(ctx);

            // then
            // 15:30 UTC on 2026-01-21 = 00:30 on 2026-01-22 in Tokyo
            assertThat(result.getYear()).isEqualTo(2026);
            assertThat(result.getMonthValue()).isEqualTo(1);
            assertThat(result.getDayOfMonth()).isEqualTo(22);
        }

        @Test
        @DisplayName("should use context zone for today calculation")
        void shouldUseContextZoneForTodayCalculation() {
            // given
            ZoneId utcZone = ZoneId.of("UTC");
            ZoneId tokyoZone = ZoneId.of("Asia/Tokyo");

            TchRequestContext ctxUtc = mock(TchRequestContext.class);
            when(ctxUtc.effectiveZoneId()).thenReturn(utcZone);

            TchRequestContext ctxTokyo = mock(TchRequestContext.class);
            when(ctxTokyo.effectiveZoneId()).thenReturn(tokyoZone);

            // when
            LocalDate dateUtc = timeProvider.today(ctxUtc);
            LocalDate dateTokyo = timeProvider.today(ctxTokyo);

            // then
            assertThat(dateUtc.getDayOfMonth()).isEqualTo(21);
            assertThat(dateTokyo.getDayOfMonth()).isEqualTo(22);
        }
    }

    @Nested
    @DisplayName("Clock testability")
    class ClockTestability {

        @Test
        @DisplayName("should use injected clock for now calculation")
        void shouldUseInjectedClockForNowCalculation() {
            // given
            Instant customInstant = Instant.parse("2025-12-31T23:59:59Z");
            Clock customClock = Clock.fixed(customInstant, ZoneId.of("UTC"));
            TimeProvider customProvider = new TimeProvider(customClock);
            ZoneId zone = ZoneId.of("UTC");

            // when
            ZonedDateTime result = customProvider.now(zone);

            // then
            assertThat(result.toInstant()).isEqualTo(customInstant);
            assertThat(result.getYear()).isEqualTo(2025);
            assertThat(result.getMonthValue()).isEqualTo(12);
            assertThat(result.getDayOfMonth()).isEqualTo(31);
        }

        @Test
        @DisplayName("should use injected clock for today calculation")
        void shouldUseInjectedClockForTodayCalculation() {
            // given
            Instant customInstant = Instant.parse("2025-12-31T23:59:59Z");
            Clock customClock = Clock.fixed(customInstant, ZoneId.of("UTC"));
            TimeProvider customProvider = new TimeProvider(customClock);
            ZoneId zone = ZoneId.of("UTC");

            // when
            LocalDate result = customProvider.today(zone);

            // then
            assertThat(result.getYear()).isEqualTo(2025);
            assertThat(result.getMonthValue()).isEqualTo(12);
            assertThat(result.getDayOfMonth()).isEqualTo(31);
        }
    }

    @Nested
    @DisplayName("Timezone edge cases")
    class TimezoneEdgeCases {

        @Test
        @DisplayName("should handle zones behind UTC correctly")
        void shouldHandleZonesBehindUtcCorrectly() {
            // given
            // Use instant at 01:00 UTC to test previous day
            Clock earlyMorningClock = Clock.fixed(
                Instant.parse("2026-01-21T01:00:00Z"),
                ZoneId.of("UTC")
            );
            TimeProvider provider = new TimeProvider(earlyMorningClock);
            ZoneId zone = ZoneId.of("America/Los_Angeles"); // UTC-8

            // when
            LocalDate result = provider.today(zone);

            // then
            // 01:00 UTC = 17:00 previous day in Los Angeles (winter)
            assertThat(result.getDayOfMonth()).isEqualTo(20);
        }

        @Test
        @DisplayName("should handle zones ahead of UTC correctly")
        void shouldHandleZonesAheadOfUtcCorrectly() {
            // given
            // Use instant at 23:00 UTC to test next day
            Clock lateEveningClock = Clock.fixed(
                Instant.parse("2026-01-21T23:00:00Z"),
                ZoneId.of("UTC")
            );
            TimeProvider provider = new TimeProvider(lateEveningClock);
            ZoneId zone = ZoneId.of("Asia/Tokyo"); // UTC+9

            // when
            LocalDate result = provider.today(zone);

            // then
            // 23:00 UTC = 08:00 next day in Tokyo
            assertThat(result.getDayOfMonth()).isEqualTo(22);
        }
    }
}
