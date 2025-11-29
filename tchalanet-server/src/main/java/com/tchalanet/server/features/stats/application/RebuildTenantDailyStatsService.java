package com.tchalanet.server.features.stats.application;

import com.tchalanet.server.features.stats.domain.model.TenantDailyStats;
import com.tchalanet.server.features.stats.domain.ports.in.RebuildTenantDailyStatsUseCase;
import com.tchalanet.server.features.stats.domain.ports.out.DrawStatsReadModelPort;
import com.tchalanet.server.features.stats.domain.ports.out.StatsRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RebuildTenantDailyStatsService implements RebuildTenantDailyStatsUseCase {

  private final DrawStatsReadModelPort drawStatsReadModel;
  private final StatsRepositoryPort statsRepository;

  // private final TenantReadModelPort tenantReadModel; // To get all tenants

  @Override
  @Transactional
  public int rebuildForDay(UUID tenantId, LocalDate day) {
    log.info("Rebuilding daily stats for tenant {} on day {}", tenantId, day);
    List<DrawStatsReadModelPort.DrawStatsSummary> drawStats =
        drawStatsReadModel.findByTenantIdAndDay(tenantId, day);

    if (drawStats.isEmpty()) {
      log.info(
          "No draw stats found for tenant {} on day {}. Skipping daily stats rebuild.",
          tenantId,
          day);
      return 0;
    }

    TenantDailyStats dailyStats = aggregateDrawStats(tenantId, day, drawStats);
    statsRepository.upsertTenantDailyStats(dailyStats);
    log.info("Successfully rebuilt daily stats for tenant {} on day {}", tenantId, day);
    return 1;
  }

  @Override
  @Transactional
  public int rebuildForAllTenantsForDay(LocalDate day) {
    log.info("Rebuilding daily stats for all tenants on day {}", day);
    // In a real scenario, you'd fetch all active tenants and loop through them,
    // calling rebuildForDay for each.
    // For now, we'll aggregate all draw stats for the day and group by tenant.

    List<DrawStatsReadModelPort.DrawStatsSummary> allDrawStatsForDay =
        drawStatsReadModel.findByDay(day);

    if (allDrawStatsForDay.isEmpty()) {
      log.info(
          "No draw stats found for day {}. Skipping daily stats rebuild for all tenants.", day);
      return 0;
    }

    Map<UUID, List<DrawStatsReadModelPort.DrawStatsSummary>> groupedByTenant =
        allDrawStatsForDay.stream()
            .collect(Collectors.groupingBy(DrawStatsReadModelPort.DrawStatsSummary::tenantId));

    int updatedCount = 0;
    for (Map.Entry<UUID, List<DrawStatsReadModelPort.DrawStatsSummary>> entry :
        groupedByTenant.entrySet()) {
      TenantDailyStats dailyStats = aggregateDrawStats(entry.getKey(), day, entry.getValue());
      statsRepository.upsertTenantDailyStats(dailyStats);
      updatedCount++;
    }
    log.info("Successfully rebuilt daily stats for {} tenants on day {}", updatedCount, day);
    return updatedCount;
  }

  private TenantDailyStats aggregateDrawStats(
      UUID tenantId, LocalDate day, List<DrawStatsReadModelPort.DrawStatsSummary> drawStats) {
    long totalTickets =
        drawStats.stream().mapToLong(DrawStatsReadModelPort.DrawStatsSummary::totalTickets).sum();
    BigDecimal totalStake =
        drawStats.stream()
            .map(DrawStatsReadModelPort.DrawStatsSummary::totalStake)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalPayout =
        drawStats.stream()
            .map(DrawStatsReadModelPort.DrawStatsSummary::totalPayout)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal grossMargin = totalStake.subtract(totalPayout);

    return new TenantDailyStats(tenantId, day, totalTickets, totalStake, totalPayout, grossMargin);
  }
}
