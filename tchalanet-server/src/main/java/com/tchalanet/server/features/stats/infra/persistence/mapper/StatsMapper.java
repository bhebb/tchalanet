package com.tchalanet.server.features.stats.infra.persistence.mapper;

import com.tchalanet.server.features.stats.domain.model.DrawStats;
import com.tchalanet.server.features.stats.domain.model.TenantDailyStats;
import com.tchalanet.server.features.stats.infra.persistence.entity.DrawStatsEntity;
import com.tchalanet.server.features.stats.infra.persistence.entity.TenantDailyStatsEntity;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class StatsMapper {

  public DrawStatsEntity toEntity(DrawStats domain) {
    DrawStatsEntity entity = new DrawStatsEntity();
    entity.setDrawId(domain.drawId());
    entity.setTenantId(domain.tenantId());
    entity.setTotalTickets(domain.totalTickets());
    entity.setTotalLines(domain.totalLines());
    entity.setTotalStake(domain.totalStake());
    entity.setTotalPayout(domain.totalPayout());
    entity.setWinnersCount(domain.winnersCount());
    entity.setLosersCount(domain.losersCount());
    entity.setGrossMargin(domain.grossMargin());
    entity.setMarginPct(domain.marginPct());
    // createdAt and updatedAt are handled by @PrePersist/@PreUpdate
    return entity;
  }

  public DrawStats toDomain(DrawStatsEntity entity) {
    return new DrawStats(
        entity.getDrawId(),
        entity.getTenantId(),
        entity.getTotalTickets(),
        entity.getTotalLines(),
        entity.getTotalStake(),
        entity.getTotalPayout(),
        entity.getWinnersCount(),
        entity.getLosersCount(),
        entity.getGrossMargin(),
        entity.getMarginPct());
  }

  public TenantDailyStatsEntity toEntity(TenantDailyStats domain) {
    TenantDailyStatsEntity entity = new TenantDailyStatsEntity();
    entity.setTenantId(domain.tenantId());
    entity.setDay(domain.day());
    entity.setTotalTickets(domain.totalTickets());
    entity.setTotalStake(domain.totalStake());
    entity.setTotalPayout(domain.totalPayout());
    entity.setGrossMargin(domain.grossMargin());
    // marginPct is calculated in the entity for simplicity or could be passed from domain
    if (domain.totalStake().compareTo(BigDecimal.ZERO) > 0) {
      entity.setMarginPct(
          domain.grossMargin().divide(domain.totalStake(), 4, BigDecimal.ROUND_HALF_UP));
    } else {
      entity.setMarginPct(BigDecimal.ZERO);
    }
    // createdAt and updatedAt are handled by @PrePersist/@PreUpdate
    return entity;
  }

  public TenantDailyStats toDomain(TenantDailyStatsEntity entity) {
    return new TenantDailyStats(
        entity.getTenantId(),
        entity.getDay(),
        entity.getTotalTickets(),
        entity.getTotalStake(),
        entity.getTotalPayout(),
        entity.getGrossMargin());
  }
}
