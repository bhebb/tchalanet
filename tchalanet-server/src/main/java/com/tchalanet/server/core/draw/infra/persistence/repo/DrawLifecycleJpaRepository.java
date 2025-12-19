package com.tchalanet.server.core.draw.infra.persistence.repo;

import com.tchalanet.server.core.draw.infra.persistence.DrawDueToCloseProjection;
import com.tchalanet.server.core.draw.infra.persistence.DrawJpaEntity;
import com.tchalanet.server.core.draw.infra.persistence.DrawOpenableProjection;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface DrawLifecycleJpaRepository extends JpaRepository<DrawJpaEntity, UUID> {

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
  List<DrawOpenableProjection> findOpenable(
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
