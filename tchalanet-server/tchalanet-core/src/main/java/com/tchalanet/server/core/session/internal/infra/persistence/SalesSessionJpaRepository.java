package com.tchalanet.server.core.session.internal.infra.persistence;

import com.tchalanet.server.core.session.internal.domain.model.SalesSessionStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalesSessionJpaRepository extends JpaRepository<SalesSessionJpaEntity, UUID> {

    Optional<SalesSessionJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    /**
     * Returns true if a non-terminal session (OPEN or CLOSED) exists for the given
     * user+outlet+businessDate. FINALIZED and CANCELLED sessions are excluded so
     * that admins can finalize a closed session and then reopen a fresh one on the
     * same business day.
     */
    @Query("""
        select count(s) > 0
        from SalesSessionJpaEntity s
        where s.tenantId  = :tenantId
          and s.outletId  = :outletId
          and s.openedBy  = :openedBy
          and s.businessDate = :businessDate
          and s.status not in (:excludedStatuses)
        """)
    boolean existsActiveForBusinessDate(
        @Param("tenantId") UUID tenantId,
        @Param("outletId") UUID outletId,
        @Param("openedBy") UUID openedBy,
        @Param("businessDate") LocalDate businessDate,
        @Param("excludedStatuses") java.util.Collection<SalesSessionStatus> excludedStatuses);

    Optional<SalesSessionJpaEntity> findByTenantIdAndTerminalIdAndStatus(
        UUID tenantId,
        UUID terminalId,
        SalesSessionStatus status);

    Optional<SalesSessionJpaEntity> findByTerminalIdAndStatus(
        UUID terminalId,
        SalesSessionStatus status);

    List<SalesSessionJpaEntity> findByTenantIdAndOutletIdAndStatus(
        UUID tenantId,
        UUID outletId,
        SalesSessionStatus status);

    List<SalesSessionJpaEntity> findByOutletIdAndStatus(
        UUID outletId,
        SalesSessionStatus status);

    @Query("""
      select s
      from SalesSessionJpaEntity s
      where s.tenantId = :tenantId
        and s.openedBy = :userId
        and s.status = :status
      """)
    Optional<SalesSessionJpaEntity> findCurrentOpenByUser(
        @Param("tenantId") UUID tenantId,
        @Param("userId") UUID userId,
        @Param("status") SalesSessionStatus status);

    @Query("""
      select s
      from SalesSessionJpaEntity s
      where s.openedBy = :userId
        and s.status = :status
      """)
    List<SalesSessionJpaEntity> findCurrentOpenByUser(
        @Param("userId") UUID userId,
        @Param("status") SalesSessionStatus status);



    @Query("""
      select s.id
      from SalesSessionJpaEntity s
      where s.outletId = :outletId
        and s.openedAt >= :from
        and s.openedAt < :to
      """)
    List<UUID> findIdsByOutletIdAndOpenedAtBetween(
        @Param("outletId") UUID outletId,
        @Param("from") Instant from,
        @Param("to") Instant to);
}
