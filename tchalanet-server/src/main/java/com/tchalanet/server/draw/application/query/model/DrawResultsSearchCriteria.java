package com.tchalanet.server.draw.application.query.model;

import java.time.ZonedDateTime;
import java.util.UUID;

public record DrawResultsSearchCriteria(
    UUID tenantId,
    String channelCode, // nullable si “tous les channels”
    ZonedDateTime from,
    ZonedDateTime to) {

  public static DrawResultsSearchCriteria today(
      UUID tenantId, String channelCode, ZonedDateTime now) {
    var start = now.toLocalDate().atStartOfDay(now.getZone());
    var end = start.plusDays(1);
    return new DrawResultsSearchCriteria(tenantId, channelCode, start, end);
  }

  public static DrawResultsSearchCriteria lastDays(
      UUID tenantId, String channelCode, ZonedDateTime now, int days) {
    var end = now;
    var start = end.minusDays(days);
    return new DrawResultsSearchCriteria(tenantId, channelCode, start, end);
  }
}
