package com.tchalanet.server.core.draw.infra.persistence.repo;

import com.tchalanet.server.core.draw.domain.model.DrawStatus;
import com.tchalanet.server.core.draw.infra.persistence.DrawJpaEntity;
import com.tchalanet.server.core.draw.infra.persistence.projection.DueToCloseProjection;
import com.tchalanet.server.core.draw.infra.persistence.projection.OpenableDrawProjection;
import org.springframework.data.jpa.repository.JpaRepository;
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
                   d.locked as locked,
                   d.scheduled_at as scheduledAt,
                   d.cutoff_at as cutoffAt
            from draw d
            where d.deleted_at is null
              and d.tenant_id = :tenantId
              and d.status = 'SCHEDULED'
              and d.locked = false
              and d.scheduled_at <= (:now + make_interval(hours => :openHorizonHours))
              and d.scheduled_at >= (:now - make_interval(hours => :openLagHours))
              and d.cutoff_at > :now
            order by d.scheduled_at asc
            limit :limit
            """,
        nativeQuery = true)
    List<OpenableDrawProjection> findOpenable(
        @Param("tenantId") UUID tenantId,
        @Param("now") Instant now,
        @Param("limit") int limit,
        @Param("openHorizonHours") int openHorizonHours,
        @Param("openLagHours") int openLagHours
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

    Optional<DrawJpaEntity> findByTenantIdAndIdAndDeletedAtIsNull(UUID tenantId, UUID id);

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
