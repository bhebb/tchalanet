package com.tchalanet.server.features.reporting;

import com.tchalanet.server.common.exception.TchValidationException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportPeriodResolverTest {

  private final ReportPeriodResolver resolver = new ReportPeriodResolver(
      Clock.fixed(Instant.parse("2026-07-10T02:00:00Z"), ZoneId.of("UTC")));

  @Test
  void defaultsToTenantLocalSevenDayWindow() {
    var period = resolver.resolve(null, null, ZoneId.of("America/Toronto"));

    assertThat(period.from()).isEqualTo(LocalDate.parse("2026-07-03"));
    assertThat(period.to()).isEqualTo(LocalDate.parse("2026-07-09"));
  }

  @Test
  void keepsExplicitWindow() {
    var period = resolver.resolve(
        LocalDate.parse("2026-07-01"),
        LocalDate.parse("2026-07-09"),
        ZoneId.of("America/Toronto"));

    assertThat(period.from()).isEqualTo(LocalDate.parse("2026-07-01"));
    assertThat(period.to()).isEqualTo(LocalDate.parse("2026-07-09"));
  }

  @Test
  void rejectsInvalidWindow() {
    assertThatThrownBy(() -> resolver.resolve(
        LocalDate.parse("2026-07-10"),
        LocalDate.parse("2026-07-09"),
        ZoneId.of("America/Toronto")))
        .isInstanceOf(TchValidationException.class)
        .hasMessage("Report period from must be before or equal to to.");
  }
}
