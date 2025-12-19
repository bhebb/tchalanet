package com.tchalanet.server.core.draw.infra.persistence.repo;

import com.tchalanet.server.core.draw.infra.persistence.DrawDueToCloseProjection;
import com.tchalanet.server.core.draw.infra.persistence.DrawJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface DrawJpaRepositoryV1 extends JpaRepository<DrawJpaEntity, UUID> {

  boolean existsByTenantIdAndDrawChannelIdAndScheduledAtAndDeletedAtIsNull(
      UUID tenantId, UUID drawChannelId, Instant scheduledAt);

  @Modifying
  @Transactional
  @Query(
      value =
          "INSERT INTO draw (tenant_id, draw_channel_id, scheduled_at, cutoff_sec, status, draw_source, system_generated, locked, created_at, updated_at, version) "
              + "VALUES (:tenantId, :drawChannelId, :scheduledAt, :cutoffSec, :status, :drawSource, :systemGenerated, :locked, now(), now(), 0) "
              + "ON CONFLICT (tenant_id, draw_channel_id, scheduled_at) DO NOTHING",
      nativeQuery = true)
  int insertIfNotExists(
      @Param("tenantId") UUID tenantId,
      @Param("drawChannelId") UUID drawChannelId,
      @Param("scheduledAt") Instant scheduledAt,
      @Param("cutoffSec") int cutoffSec,
      @Param("status") String status,
      @Param("drawSource") String drawSource,
      @Param("systemGenerated") boolean systemGenerated,
      @Param("locked") boolean locked);

  @Query(
      value =
          """
            select d.tenant_id as tenantId, d.id as drawId, d.locked as locked
            from draw d
            where d.deleted_at is null
              and d.status = 'OPEN'
              and (d.scheduled_at - (d.cutoff_sec || ' seconds')::interval) <= :now
            order by d.scheduled_at asc
            limit :limit
            """,
      nativeQuery = true)
  List<DrawDueToCloseProjection> findDueToClose(@Param("now") Instant now, @Param("limit") int limit);

  @Modifying
  @Transactional
  @Query(
      value =
          """
            update draw
            set status='CLOSED', updated_at=now()
            where deleted_at is null
              and locked=false
              and id = any(:ids)
              and status in ('SCHEDULED','OPEN')
            """,
      nativeQuery = true)
  int bulkClose(@Param("ids") UUID[] ids);
}
