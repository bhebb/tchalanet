package com.tchalanet.server.features.stats.infra.persistence.adapter;

import com.tchalanet.server.features.stats.domain.ports.out.DrawStatsReadModelPort;
import com.tchalanet.server.features.stats.infra.persistence.entity.DrawStatsEntity;
import com.tchalanet.server.features.stats.infra.persistence.repository.SpringDrawStatsJpaRepository;
import java.time.LocalDate;
import java.time.ZoneOffset; // Assuming UTC for simplicity in DB, adjust if needed
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaDrawStatsReadModelAdapter implements DrawStatsReadModelPort {

  private final SpringDrawStatsJpaRepository drawStatsJpaRepository;

  @Override
  public List<DrawStatsSummary> findByTenantIdAndDay(UUID tenantId, LocalDate day) {
    // This requires a new method in SpringDrawStatsJpaRepository
    // For now, we'll fetch all and filter, but a direct query is better for performance.
    return drawStatsJpaRepository
        .findByTenantIdAndCreatedAtBetween(
            tenantId,
            day.atStartOfDay().toInstant(ZoneOffset.UTC),
            day.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC))
        .stream()
        .map(this::toSummary)
        .collect(Collectors.toList());
  }

  @Override
  public List<DrawStatsSummary> findByDay(LocalDate day) {
    // This requires a new method in SpringDrawStatsJpaRepository
    return drawStatsJpaRepository
        .findByCreatedAtBetween(
            day.atStartOfDay().toInstant(ZoneOffset.UTC),
            day.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC))
        .stream()
        .map(this::toSummary)
        .collect(Collectors.toList());
  }

  private DrawStatsSummary toSummary(DrawStatsEntity entity) {
    return new DrawStatsSummary(
        entity.getDrawId(),
        entity.getTenantId(),
        entity.getTotalTickets(),
        entity.getTotalStake(),
        entity.getTotalPayout());
  }
}
