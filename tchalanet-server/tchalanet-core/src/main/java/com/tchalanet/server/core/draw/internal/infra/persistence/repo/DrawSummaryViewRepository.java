package com.tchalanet.server.core.draw.internal.infra.persistence.repo;

import com.tchalanet.server.core.draw.internal.domain.model.DrawStatus;
import com.tchalanet.server.core.draw.internal.infra.persistence.view.DrawSummaryViewEntity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;
@Repository
public interface DrawSummaryViewRepository extends JpaRepository<DrawSummaryViewEntity, UUID> {

    Optional<DrawSummaryViewEntity> findByTenantIdAndDrawId(UUID tenantId, UUID drawId);

    @Query("""
        select v
        from DrawSummaryViewEntity v
        where v.tenantId = :tenantId
          and (:resultSlotId is null or v.resultSlotId = :resultSlotId)
          and (:status is null or v.status = :status)
          and (:fromDate is null or v.drawDate >= :fromDate)
          and (:toDate is null or v.drawDate <= :toDate)
        """)
    Page<DrawSummaryViewEntity> search(
        @Param("tenantId") UUID tenantId,
        @Param("resultSlotId") UUID resultSlotId,
        @Param("status") DrawStatus status,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate,
        Pageable pageable
    );

    @Query("""
        select v
        from DrawSummaryViewEntity v
        where v.tenantId = :tenantId
          and (:resultSlotId is null or v.resultSlotId = :resultSlotId)
          and v.cutoffAt >= :now
          and v.scheduledAt < :until
          and v.status in :statuses
        order by v.scheduledAt asc
        """)
    Page<DrawSummaryViewEntity> next(
        @Param("tenantId") UUID tenantId,
        @Param("resultSlotId") UUID resultSlotId,
        @Param("now") Instant now,
        @Param("until") Instant until,
        @Param("statuses") List<DrawStatus> statuses,
        Pageable pageable
    );

    @Query("""
        select v
        from DrawSummaryViewEntity v
        where v.tenantId = :tenantId
          and v.drawResultId is not null
          and (:resultSlotKeysEmpty = true or v.resultSlotKey in :resultSlotKeys)
        order by v.resultedAt desc nulls last, v.scheduledAt desc
        """)
    Page<DrawSummaryViewEntity> latestWithResults(
        @Param("tenantId") UUID tenantId,
        @Param("resultSlotKeys") List<String> resultSlotKeys,
        @Param("resultSlotKeysEmpty") boolean resultSlotKeysEmpty,
        Pageable pageable
    );

    @Query("""
        select v
        from DrawSummaryViewEntity v
        where v.tenantId = :tenantId
          and v.drawResultId = :drawResultId
        order by v.scheduledAt asc
        """)
    List<DrawSummaryViewEntity> findByTenantIdAndDrawResultId(
        @Param("tenantId") UUID tenantId,
        @Param("drawResultId") UUID drawResultId
    );

    @Query("""
        select v
        from DrawSummaryViewEntity v
        where v.tenantId = :tenantId
          and v.status = 'RESULTED'
          and v.drawResultStatus = 'PROVISIONAL'
          and v.resultedAt < :threshold
        order by v.resultedAt asc
        """)
    List<DrawSummaryViewEntity> findResultedProvisionalOlderThan(
        @Param("tenantId") UUID tenantId,
        @Param("threshold") Instant threshold
    );
}
