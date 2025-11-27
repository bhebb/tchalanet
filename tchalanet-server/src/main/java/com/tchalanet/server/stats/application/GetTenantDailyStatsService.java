package com.tchalanet.server.stats.application;

import com.tchalanet.server.stats.domain.model.TenantDailyStats;
import com.tchalanet.server.stats.domain.ports.in.GetTenantDailyStatsQuery;
import com.tchalanet.server.stats.infra.persistence.mapper.StatsMapper;
import com.tchalanet.server.stats.infra.persistence.repository.SpringTenantDailyStatsJpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetTenantDailyStatsService implements GetTenantDailyStatsQuery {

  private final SpringTenantDailyStatsJpaRepository tenantDailyStatsJpaRepository;
  private final StatsMapper mapper;

  @Override
  public List<TenantDailyStats> getDailyStatsForTenant(
      UUID tenantId, LocalDate from, LocalDate to) {
    // This requires a new method in SpringTenantDailyStatsJpaRepository
    // For now, we'll fetch all and filter, but a direct query is better for performance.
    return tenantDailyStatsJpaRepository.findByTenantIdAndDayBetween(tenantId, from, to).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }
}
