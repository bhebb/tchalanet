package com.tchalanet.server.draw.infra.persistence.repository;

import com.tchalanet.server.draw.domain.model.DrawStatus;
import com.tchalanet.server.draw.infra.persistence.entity.DrawEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDrawJpaRepository extends JpaRepository<DrawEntity, UUID> {
  List<DrawEntity> findByTenantIdAndScheduledAtBetween(UUID tenantId, Instant from, Instant to);

  boolean existsByTenantIdAndDrawChannelIdAndScheduledAt(
      UUID tenantId, UUID drawChannelId, Instant scheduledAt);

  List<DrawEntity> findByStatusAndScheduledAtBefore(DrawStatus status, Instant before);

  @Query(
      "SELECT d FROM DrawEntity d WHERE d.tenantId = :tenantId AND d.status = 'SCHEDULED' AND (d.scheduledAt - d.cutoffSec * 1000) < :now")
  List<DrawEntity> findScheduledDrawsPastCutoff(
      @Param("tenantId") UUID tenantId, @Param("now") Instant now);
}
