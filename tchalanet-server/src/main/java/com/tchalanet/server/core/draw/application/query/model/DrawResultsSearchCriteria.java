package com.tchalanet.server.core.draw.application.query.model;

import com.tchalanet.server.common.types.id.TenantId;
import java.time.ZonedDateTime;

public record DrawResultsSearchCriteria(
    TenantId tenantId,
    String channelCode, // nullable si “tous les channels”
    ZonedDateTime from,
    ZonedDateTime to) {

  public static DrawResultsSearchCriteria today(
      TenantId tenantId, String channelCode, ZonedDateTime now) {
    var start = now.toLocalDate().atStartOfDay(now.getZone());
    var end = start.plusDays(1);
    return new DrawResultsSearchCriteria(tenantId, channelCode, start, end);
  }

  public static DrawResultsSearchCriteria lastDays(
      TenantId tenantId, String channelCode, ZonedDateTime now, int days) {
    var end = now;
    var start = end.minusDays(days);
    return new DrawResultsSearchCriteria(tenantId, channelCode, start, end);
  }
}
