package com.tchalanet.server.features.stats.aggregates.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatsEventLogJpaRepository extends JpaRepository<StatsEventLogEntity, UUID> {

  List<StatsEventLogEntity> findByProcessedAtBetween(Instant from, Instant to);

  List<StatsEventLogEntity> findByEventType(String eventType);
}
