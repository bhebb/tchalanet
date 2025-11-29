package com.tchalanet.server.draw.infra.persistence;

import com.tchalanet.server.draw.domain.model.DrawStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface DrawJpaRepository extends JpaRepository<DrawJpaEntity, UUID> {
  List<DrawJpaEntity> findByTenantIdAndScheduledAtBetweenOrderByScheduledAt(
      UUID tenantId, Instant from, Instant to);

  // finds draws ready to be settled: status = 'CLOSED' and scheduled_at <= before
  List<DrawJpaEntity> findByStatusAndScheduledAtBeforeAndDeletedAtIsNull(
      String status, Instant before);

  boolean existsByTenantIdAndDrawChannelIdAndScheduledAt(
      UUID tenantId, UUID drawChannelId, Instant scheduledAt);

  // Find draws to settle, excluding locked entries (manual checks are performed before update)
  List<DrawJpaEntity> findByStatusAndScheduledAtBeforeAndDeletedAtIsNullAndLockedFalse(
      String status, Instant before);

  @Modifying
  @Transactional
  @Query(
      value =
          "INSERT INTO draw (id, tenant_id, draw_channel_id, game_code, scheduled_at, cutoff_sec, status, result_payload, created_at, updated_at, version) VALUES (:id, :tenantId, :drawChannelId, :gameCode, :scheduledAt, :cutoffSec, :status, CAST(:resultPayload AS jsonb), now(), now(), 0) ON CONFLICT (tenant_id, draw_channel_id, scheduled_at) DO NOTHING",
      nativeQuery = true)
  int insertIfNotExists(
      @Param("id") UUID id,
      @Param("tenantId") UUID tenantId,
      @Param("drawChannelId") UUID drawChannelId,
      @Param("gameCode") String gameCode,
      @Param("scheduledAt") Instant scheduledAt,
      @Param("cutoffSec") Integer cutoffSec,
      @Param("status") String status,
      @Param("resultPayload") String resultPayload);

  List<DrawJpaEntity> findByTenantIdAndScheduledAtBetween(UUID tenantId, Instant from, Instant to);

  List<DrawJpaEntity> findByStatusAndScheduledAtBefore(DrawStatus status, Instant before);

  @Query(
      "SELECT d FROM DrawJpaEntity d WHERE d.tenantId = :tenantId AND d.status = 'SCHEDULED' AND (d.scheduledAt - d.cutoffSec * 1000) < :now")
  List<DrawJpaEntity> findScheduledDrawsPastCutoff(
      @Param("tenantId") UUID tenantId, @Param("now") Instant now);

  List<DrawJpaEntity>
      findByTenantIdAndDrawChannelCodeAndStatusAndScheduledAtAfterOrderByScheduledAtAsc(
          UUID tenantId, String drawChannelId, String status, Instant scheduledAtAfter);
}
