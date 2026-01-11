package com.tchalanet.server.core.draw.infra.persistence.repo;

import com.tchalanet.server.core.draw.infra.persistence.DrawJpaEntity;
import com.tchalanet.server.core.draw.infra.persistence.projection.DueToCloseProjection;
import com.tchalanet.server.core.draw.infra.persistence.projection.OpenableDrawProjection;
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

  @Query(
      """
          select d
          from DrawJpaEntity d
          join fetch d.drawChannel ch
          where d.tenantId = :tenantId
            and d.deletedAt is null
            and d.scheduledAt between :from and :to
          order by d.scheduledAt asc
        """)
  List<DrawJpaEntity> findSummariesWithChannelAndResult(
      @Param("tenantId") UUID tenantId, @Param("from") Instant from, @Param("to") Instant to);

  // JPQL variant that fetches drawChannel to avoid LazyInitializationException in adapters
  @Query(
      "select d from DrawJpaEntity d left join fetch d.drawChannel ch where d.tenantId = :tenantId and d.scheduledAt between :from and :to order by d.scheduledAt asc")
  List<DrawJpaEntity> findByTenantIdAndScheduledAtBetweenFetchChannelOrderByScheduledAt(
      @Param("tenantId") UUID tenantId, @Param("from") Instant from, @Param("to") Instant to);

  // Find draws to settle, excluding locked entries (manual checks are performed before update)
  List<DrawJpaEntity> findByStatusAndScheduledAtBeforeAndDeletedAtIsNullAndLockedFalse(
      String status, Instant before);

  @Query(
      value =
          """
                  select d.tenant_id as tenantId, d.id as drawId, d.locked as locked
                  from draw d
                  where d.deleted_at is null
                    and d.status = 'OPEN'
                    and d.cutoff_at <= to_timestamp(:nowEpoch)
                    and d.locked = false
                  order by d.cutoff_at asc
                  limit :limit
                """,
      nativeQuery = true)
  List<DueToCloseProjection> findDueToClose(
      @Param("nowEpoch") long nowEpoch, @Param("limit") int limit);

  @Modifying
  @Transactional
  @Query(
      value =
          """
                update draw
                set status='CLOSED',
                    closed_at = now(),
                    updated_at=now()
                where deleted_at is null
                  and locked=false
                  and id = any(:ids)
                  and status = 'OPEN'
                """,
      nativeQuery = true)
  int bulkClose(@Param("ids") UUID[] ids);

  // --- lifecycle queries migrated from DrawLifecycleJpaRepository ---

  @Query(
      value =
          """
                  select d.tenant_id as tenantId,
                         d.id as drawId,
                         d.locked as locked,
                         d.scheduled_at as scheduledAt,
                         d.cutoff_at as cutoffAt
                  from draw d
                  where d.deleted_at is null
                    and d.status='SCHEDULED'
                    and d.locked=false
                    and d.scheduled_at <= (to_timestamp(:nowEpoch) + make_interval(hours => :openHorizonHours))
                    and d.scheduled_at >= (to_timestamp(:nowEpoch) - make_interval(hours => :openLagHours))
                    and d.cutoff_at > to_timestamp(:nowEpoch)
                  order by d.scheduled_at asc
                  limit :limit
                """,
      nativeQuery = true)
  List<OpenableDrawProjection> findOpenable(
      @Param("nowEpoch") long nowEpoch,
      @Param("limit") int limit,
      @Param("openHorizonHours") int openHorizonHours,
      @Param("openLagHours") int openLagHours);

  @Modifying
  @Transactional
  @Query(
      value =
          """
                update draw
                set status='OPEN',
                    opened_at = now(),
                    updated_at=now()
                where deleted_at is null
                  and locked=false
                  and status='SCHEDULED'
                  and id = any(:ids)
                """,
      nativeQuery = true)
  int bulkOpen(@Param("ids") UUID[] ids);
}
