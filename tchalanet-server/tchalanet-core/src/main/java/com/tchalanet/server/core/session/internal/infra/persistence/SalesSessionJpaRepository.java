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

    boolean existsByTenantIdAndOutletIdAndOpenedByAndBusinessDate(
        UUID tenantId,
        UUID outletId,
        UUID openedBy,
        LocalDate businessDate);

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
