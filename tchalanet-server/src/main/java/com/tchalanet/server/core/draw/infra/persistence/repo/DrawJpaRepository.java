package com.tchalanet.server.core.draw.infra.persistence.repo;

import com.tchalanet.server.core.draw.domain.model.DrawStatus;
import com.tchalanet.server.core.draw.infra.persistence.DrawJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

  @Query(
      """
          select d from DrawJpaEntity d
          where d.tenantId = :tenantId
            and d.scheduledAt between :from and :to
          order by d.scheduledAt asc
        """)
  List<DrawJpaEntity> listByRange(UUID tenantId, Instant from, Instant to);

  @Query(
      """
          select d from DrawJpaEntity d
          where d.tenantId = :tenantId and d.id = :id
        """)
  Optional<DrawJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

  boolean existsByTenantIdAndDrawChannel_IdAndScheduledAtAndDeletedAtIsNull(
      UUID tenantId, UUID drawChannelId, Instant scheduledAt);

  @Query(
      value =
          """
            select d.tenant_id as tenantId, d.id as drawId, d.locked as locked
            from draw d
            where d.deleted_at is null
              and d.locked=false
              and d.status in ('SCHEDULED','OPEN')
              and (d.scheduled_at - (d.cutoff_sec || ' seconds')::interval) <= :now
            order by d.scheduled_at asc
            limit :limit
            """,
      nativeQuery = true)
  List<Object[]> findDueToClose(
      @Param("tenantId") UUID tenantId, @Param("now") Instant now, @Param("limit") int limit);

  @Modifying
  @Transactional
  @Query(
      value =
          """
            update draw
            set status='CLOSED', updated_at=now()
            where tenant_id=:tenantId
              and deleted_at is null
              and locked=false
              and id = any(:ids)
              and status in ('SCHEDULED','OPEN')
            """,
      nativeQuery = true)
  int bulkClose(@Param("tenantId") UUID tenantId, @Param("ids") UUID[] ids);

  // --- lifecycle queries migrated from DrawLifecycleJpaRepository ---

  @Query(
      value =
          """
            select d.tenant_id as tenantId, d.id as drawId, d.locked as locked, d.scheduled_at as scheduledAt, d.cutoff_sec as cutoffSec
            from draw d
            where d.deleted_at is null
              and d.locked=false
              and d.status='SCHEDULED'
              and (d.scheduled_at - (d.cutoff_sec || ' seconds')::interval) > :now
              and d.scheduled_at <= (:now + (:openHorizonHours || ' hours')::interval)
              and d.scheduled_at >= (:now - (:openLagHours || ' hours')::interval)
            order by d.scheduled_at asc
            limit :limit
            """,
      nativeQuery = true)
  List<Object[]> findOpenable(
      @Param("now") Instant now,
      @Param("limit") int limit,
      @Param("openHorizonHours") int openHorizonHours,
      @Param("openLagHours") int openLagHours);

  @Modifying
  @Transactional
  @Query(
      value =
          """
            update draw
            set status='OPEN', updated_at=now()
            where deleted_at is null
              and locked=false
              and status='SCHEDULED'
              and id = any(:ids)
            """,
      nativeQuery = true)
  int bulkOpen(@Param("ids") UUID[] ids);
}
