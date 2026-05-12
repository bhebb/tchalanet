package com.tchalanet.server.features.stats.aggregates.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatsDrawJpaRepository extends JpaRepository<StatsDrawEntity, UUID> {

  List<StatsDrawEntity> findByDrawId(UUID drawId);

  List<StatsDrawEntity> findByTenantIdAndScheduledAtBetween(
      UUID tenantId, Instant from, Instant to);
}
