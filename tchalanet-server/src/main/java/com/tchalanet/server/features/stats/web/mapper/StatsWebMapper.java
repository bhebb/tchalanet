package com.tchalanet.server.features.stats.web.mapper;

import com.tchalanet.server.features.stats.domain.model.DrawStats;
import com.tchalanet.server.features.stats.domain.model.TenantDailyStats;
import com.tchalanet.server.features.stats.web.dto.DrawStatsResponse;
import com.tchalanet.server.features.stats.web.dto.TenantDailyStatsResponse;
import org.springframework.stereotype.Component;

@Component
public class StatsWebMapper {

  public DrawStatsResponse toDrawStatsResponse(DrawStats domain) {
    return new DrawStatsResponse(
        domain.drawId(),
        domain.tenantId(),
        domain.totalTickets(),
        domain.totalLines(),
        domain.totalStake(),
        domain.totalPayout(),
        domain.winnersCount(),
        domain.losersCount(),
        domain.grossMargin(),
        domain.marginPct());
  }

  public TenantDailyStatsResponse toTenantDailyStatsResponse(TenantDailyStats domain) {
    return new TenantDailyStatsResponse(
        domain.tenantId(),
        domain.day(),
        domain.totalTickets(),
        domain.totalStake(),
        domain.totalPayout(),
        domain.grossMargin());
  }
}
