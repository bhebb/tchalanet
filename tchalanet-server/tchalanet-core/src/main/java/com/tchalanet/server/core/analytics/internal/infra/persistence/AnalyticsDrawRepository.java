package com.tchalanet.server.core.analytics.internal.infra.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Repository for {@code analytics_draw}. */
@Repository
public interface AnalyticsDrawRepository extends JpaRepository<AnalyticsDrawEntity, UUID> {

  Optional<AnalyticsDrawEntity> findByDrawId(UUID drawId);

  List<AnalyticsDrawEntity> findByTenantIdAndRefDateBetweenOrderByRefDate(
      UUID tenantId, LocalDate from, LocalDate to);

  @Transactional
  @Modifying
  @Query("DELETE FROM AnalyticsDrawEntity a WHERE a.refDate < :cutoff")
  int deleteOlderThan(@Param("cutoff") LocalDate cutoff);

  @Query("SELECT COUNT(a) FROM AnalyticsDrawEntity a WHERE a.refDate < :cutoff")
  long countOlderThan(@Param("cutoff") LocalDate cutoff);
}
