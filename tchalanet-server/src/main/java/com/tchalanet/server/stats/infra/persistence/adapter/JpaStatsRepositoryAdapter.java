package com.tchalanet.server.stats.infra.persistence.adapter;

import com.tchalanet.server.stats.domain.model.DrawStats;
import com.tchalanet.server.stats.domain.model.TenantDailyStats;
import com.tchalanet.server.stats.domain.ports.out.StatsRepositoryPort;
import com.tchalanet.server.stats.infra.persistence.mapper.StatsMapper;
import com.tchalanet.server.stats.infra.persistence.repository.SpringDrawStatsJpaRepository;
import com.tchalanet.server.stats.infra.persistence.repository.SpringTenantDailyStatsJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class JpaStatsRepositoryAdapter implements StatsRepositoryPort {

  private final SpringDrawStatsJpaRepository drawStatsJpaRepository;
  private final SpringTenantDailyStatsJpaRepository tenantDailyStatsJpaRepository;
  private final StatsMapper mapper;

  @Override
  @Transactional
  public void upsertDrawStats(DrawStats drawStats) {
    drawStatsJpaRepository.save(mapper.toEntity(drawStats));
  }

  @Override
  @Transactional
  public void upsertTenantDailyStats(TenantDailyStats tenantDailyStats) {
    tenantDailyStatsJpaRepository.save(mapper.toEntity(tenantDailyStats));
  }
}
