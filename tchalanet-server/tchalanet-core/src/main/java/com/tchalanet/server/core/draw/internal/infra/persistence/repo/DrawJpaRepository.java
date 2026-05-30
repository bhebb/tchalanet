package com.tchalanet.server.core.draw.internal.infra.persistence.repo;

import com.tchalanet.server.core.draw.api.model.DrawStatus;
import com.tchalanet.server.core.draw.internal.infra.persistence.DrawJpaEntity;
import com.tchalanet.server.core.draw.internal.infra.persistence.projection.DueToCloseProjection;
import com.tchalanet.server.core.draw.internal.infra.persistence.projection.OpenableDrawProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DrawJpaRepository extends JpaRepository<DrawJpaEntity, UUID> {

    @Query(
        value = """
            select d.tenant_id as tenantId,
                   d.id as drawId,
                   d.locked as locked
            from draw d
            where d.deleted_at is null
              and d.tenant_id = :tenantId
              and d.status = 'OPEN'
              and d.cutoff_at <= :now
              and d.locked = false
            order by d.cutoff_at asc
            limit :limit
            """,
        nativeQuery = true)
    List<DueToCloseProjection> findDueToClose(
        @Param("tenantId") UUID tenantId,
        @Param("now") Instant now,
        @Param("limit") int limit
    );

    @Modifying
    @Query(
        value = """
            update draw
            set status = 'CLOSED',
                closed_at = :now,
                updated_at = :now
            where deleted_at is null
              and locked = false
              and id = any(:ids)
              and status = 'OPEN'
            """,
        nativeQuery = true)
    int bulkClose(@Param("ids") UUID[] ids, @Param("now") Instant now);

    @Query(
        value = """
            select d.tenant_id as tenantId,
                   d.id as drawId,
                   dc.result_slot_id as resultSlotId,
                   d.draw_date as drawDate,
                   d.locked as locked,
                   d.scheduled_at as scheduledAt,
                   d.cutoff_at as cutoffAt
            from draw d
            join draw_channel dc on dc.id = d.draw_channel_id
            where d.deleted_at is null
              and d.tenant_id = :tenantId
              and d.status = 'SCHEDULED'
              and d.locked = false
              and d.scheduled_at <= :windowEnd
              and d.scheduled_at >= :windowStart
              and d.cutoff_at > :now
            order by d.scheduled_at asc
            limit :limit
            """,
        nativeQuery = true)
    List<OpenableDrawProjection> findOpenable(
        @Param("tenantId") UUID tenantId,
        @Param("now") Instant now,
        @Param("limit") int limit,
        @Param("windowStart") Instant windowStart,
        @Param("windowEnd") Instant windowEnd
    );

    List<DrawJpaEntity> findByDrawResultId(UUID drawResultId);

    List<DrawJpaEntity> findByTenantIdAndDrawDate(UUID tenantId, java.time.LocalDate drawDate);

    @Query(
        value = """
            select d.tenant_id as tenantId,
                   d.id as drawId,
                   dc.result_slot_id as resultSlotId,
                   d.draw_date as drawDate,
                   d.locked as locked,
                   d.scheduled_at as scheduledAt,
                   d.cutoff_at as cutoffAt
            from draw d
            join draw_channel dc on dc.id = d.draw_channel_id
            where d.deleted_at is null
              and dc.deleted_at is null
          and dc.active = true
              and d.tenant_id = :tenantId
              and d.status = 'SCHEDULED'
          and d.opened_at is null
              and d.locked = false
              and d.draw_date = :drawDate
          and ((d.draw_date + coalesce(dc.sales_open_time, :defaultSalesOpenTime)) at time zone dc.timezone) <= :now
              and d.cutoff_at > :now
            order by d.scheduled_at asc
            limit :limit
            """,
        nativeQuery = true)
    List<OpenableDrawProjection> findOpenableForSalesOpenTime(
        @Param("tenantId") UUID tenantId,
        @Param("now") Instant now,
        @Param("drawDate") java.time.LocalDate drawDate,
        @Param("defaultSalesOpenTime") java.time.LocalTime defaultSalesOpenTime,
        @Param("limit") int limit
    );

    @Query(
        value = """
            select d.tenant_id as tenantId,
                   d.id as drawId,
                   dc.result_slot_id as resultSlotId,
                   d.draw_date as drawDate,
                   d.locked as locked,
                   d.scheduled_at as scheduledAt,
                   d.cutoff_at as cutoffAt
            from draw d
            join draw_channel dc on dc.id = d.draw_channel_id
            where d.deleted_at is null
              and dc.deleted_at is null
              and dc.active = true
              and d.tenant_id = :tenantId
              and d.status = 'SCHEDULED'
              and d.opened_at is null
              and d.locked = false
              and d.draw_date = (:now at time zone dc.timezone)::date
              and ((d.draw_date + coalesce(dc.sales_open_time, :defaultSalesOpenTime)) at time zone dc.timezone) <= :now
              and d.cutoff_at > :now
            order by d.scheduled_at asc
            limit :limit
            """,
        nativeQuery = true)
    List<OpenableDrawProjection> findOpenableForEffectiveToday(
        @Param("tenantId") UUID tenantId,
        @Param("now") Instant now,
        @Param("defaultSalesOpenTime") java.time.LocalTime defaultSalesOpenTime,
        @Param("limit") int limit
    );

    @Modifying
    @Query(
        value = """
            update draw
            set status = 'OPEN',
                opened_at = :now,
                updated_at = :now
            where deleted_at is null
              and locked = false
              and status = 'SCHEDULED'
              and id = any(:ids)
            """,
        nativeQuery = true)
    int bulkOpen(@Param("ids") UUID[] ids, @Param("now") Instant now);

    @Modifying
    @Query(
        value = """
            update draw
            set status = 'CANCELED',
                canceled_at = :now,
                cancel_reason_code = :reasonCode,
                cancel_reason_label = :reasonLabel,
                updated_at = :now
            where deleted_at is null
              and locked = false
              and status = 'SCHEDULED'
              and id = any(:ids)
            """,
        nativeQuery = true)
    int bulkCancelScheduled(
        @Param("ids") UUID[] ids,
        @Param("reasonCode") String reasonCode,
        @Param("reasonLabel") String reasonLabel,
        @Param("now") Instant now);

    Optional<DrawJpaEntity> findByTenantIdAndIdAndDeletedAtIsNull(UUID tenantId, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DrawJpaEntity d where d.tenantId = :tenantId and d.id = :id and d.deletedAt is null")
    Optional<DrawJpaEntity> findByTenantIdAndIdAndDeletedAtIsNullForUpdate(
        @Param("tenantId") UUID tenantId,
        @Param("id") UUID id
    );

    boolean existsByTenantIdAndDrawResultIdAndStatusAndDeletedAtIsNull(
        UUID tenantId,
        UUID drawResultId,
        DrawStatus status
    );

    List<DrawJpaEntity> findByTenantIdAndDrawResultIdAndDeletedAtIsNull(
        UUID tenantId,
        UUID drawResultId
    );

    List<DrawJpaEntity> findAllByTenantIdAndIdInAndDeletedAtIsNull(
        UUID tenantId,
        List<UUID> ids
    );
}
