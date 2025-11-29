package com.tchalanet.server.core.draw.application.query.model;

import java.time.LocalDate;
import java.util.UUID;

public record DrawSearchCriteria(
    UUID tenantId, String channelCode, LocalDate from, LocalDate to, int days) {

  public static DrawSearchCriteria today(UUID tenantId, String channelCode) {
    LocalDate today = LocalDate.now();
    return new DrawSearchCriteria(tenantId, channelCode, today, today, 1);
  }

  public static DrawSearchCriteria lastDays(UUID tenantId, String channelCode, int days) {
    LocalDate to = LocalDate.now();
    LocalDate from = to.minusDays(days);
    return new DrawSearchCriteria(tenantId, channelCode, from, to, days);
  }

  public DrawSearchCriteria(UUID tenantId, String channelCode, LocalDate from, LocalDate to) {
    this(tenantId, channelCode, from, to, (int) (to.toEpochDay() - from.toEpochDay()));
  }
}
