package com.tchalanet.server.features.stats.infra.persistence.repository;

import com.tchalanet.server.features.stats.infra.persistence.entity.TenantDailyStatsEntity;
import com.tchalanet.server.features.stats.infra.persistence.entity.TenantDailyStatsId;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringTenantDailyStatsJpaRepository
    extends JpaRepository<TenantDailyStatsEntity, TenantDailyStatsId> {
  List<TenantDailyStatsEntity> findByTenantIdAndDayBetween(
      UUID tenantId, LocalDate from, LocalDate to);
}
