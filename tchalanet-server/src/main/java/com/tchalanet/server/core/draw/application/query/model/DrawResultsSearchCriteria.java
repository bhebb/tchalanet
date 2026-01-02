package com.tchalanet.server.core.draw.application.query.model;

import com.tchalanet.server.common.types.id.TenantId;
import java.time.ZonedDateTime;

public record DrawResultsSearchCriteria(
    TenantId tenantId,
    String channelCode, // nullable si “tous les channels”
    ZonedDateTime from,
    ZonedDateTime to,
    Integer page,
    Integer size) {

  public static DrawResultsSearchCriteria today(
      TenantId tenantId, String channelCode, ZonedDateTime now) {
    var start = now.toLocalDate().atStartOfDay(now.getZone());
    return new DrawResultsSearchCriteria(
        tenantId, channelCode, start, start.plusDays(1), null, null);
  }

  public static DrawResultsSearchCriteria lastDays(
      TenantId tenantId, String channelCode, ZonedDateTime now, int days) {
    var start = now.minusDays(days);
    return new DrawResultsSearchCriteria(tenantId, channelCode, start, now, null, null);
  }

  public DrawResultsSearchCriteria withPageAndSize(Integer page, Integer size) {
    return new DrawResultsSearchCriteria(
        this.tenantId, this.channelCode, this.from, this.to, page, size);
  }
}
