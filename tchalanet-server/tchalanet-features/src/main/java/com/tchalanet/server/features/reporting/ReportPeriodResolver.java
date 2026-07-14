package com.tchalanet.server.features.reporting;

import com.tchalanet.server.common.exception.TchValidationException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class ReportPeriodResolver {

  private static final int DEFAULT_DAYS = 6;

  private final Clock clock;

  public ReportPeriodResolver(Clock clock) {
    this.clock = clock;
  }

  public ReportPeriod resolve(LocalDate from, LocalDate to, ZoneId zoneId) {
    ZoneId effectiveZone = zoneId != null ? zoneId : ZoneId.systemDefault();
    LocalDate today = LocalDate.now(clock.withZone(effectiveZone));
    LocalDate toDate = to != null ? to : today;
    LocalDate fromDate = from != null ? from : toDate.minusDays(DEFAULT_DAYS);
    validate(fromDate, toDate);
    return new ReportPeriod(fromDate, toDate);
  }

  public ReportPeriod resolve(
      LocalDate from, LocalDate to, LocalDate fallbackFrom, LocalDate fallbackTo) {
    LocalDate toDate = to != null ? to : fallbackTo;
    LocalDate fromDate = from != null ? from : fallbackFrom;
    validate(fromDate, toDate);
    return new ReportPeriod(fromDate, toDate);
  }

  private static void validate(LocalDate from, LocalDate to) {
    if (from == null || to == null) {
      throw new TchValidationException(
          "report.period.required", "Report period from/to must be provided.");
    }
    if (from.isAfter(to)) {
      throw new TchValidationException(
          "report.period.invalid", "Report period from must be before or equal to to.");
    }
  }
}
