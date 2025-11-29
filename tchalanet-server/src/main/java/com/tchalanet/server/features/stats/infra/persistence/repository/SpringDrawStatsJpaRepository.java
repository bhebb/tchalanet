package com.tchalanet.server.features.stats.infra.persistence.repository;

import com.tchalanet.server.features.stats.infra.persistence.entity.DrawStatsEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDrawStatsJpaRepository extends JpaRepository<DrawStatsEntity, UUID> {
  List<DrawStatsEntity> findByTenantIdAndCreatedAtBetween(
      UUID tenantId, Instant startOfDay, Instant endOfDay);

  List<DrawStatsEntity> findByCreatedAtBetween(Instant startOfDay, Instant endOfDay);
}
