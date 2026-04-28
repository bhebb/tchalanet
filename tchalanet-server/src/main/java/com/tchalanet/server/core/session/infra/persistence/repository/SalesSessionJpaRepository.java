package com.tchalanet.server.core.session.infra.persistence.repository;

import com.tchalanet.server.core.session.infra.persistence.entity.SalesSessionJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SalesSessionJpaRepository extends JpaRepository<SalesSessionJpaEntity, UUID> {

  /** Finds an open POS session for the given tenant and terminal. */
  Optional<SalesSessionJpaEntity> findByTenantIdAndTerminalIdAndStatus(
      @Param("tenantId") UUID tenantId,
      @Param("terminalId") UUID terminalId,
      @Param("status") com.tchalanet.server.core.session.domain.model.SalesSessionStatus status);

  /** Finds an open POS session for the given terminal. RLS should handle tenant. */
  Optional<SalesSessionJpaEntity> findByTerminalIdAndStatus(
      @Param("terminalId") UUID terminalId,
      @Param("status") com.tchalanet.server.core.session.domain.model.SalesSessionStatus status);

  /** Finds all open sessions for a cashier. */
  @Query(
      "SELECT s FROM SalesSessionJpaEntity s WHERE s.tenantId = :tenantId AND s.userId = :userId AND s.status = 'OPENED'")
  java.util.List<SalesSessionJpaEntity> findOpenByCashier(
      @Param("tenantId") UUID tenantId, @Param("userId") UUID userId);

  /** Finds all open sessions for a cashier. RLS version. */
  @Query(
      "SELECT s FROM SalesSessionJpaEntity s WHERE s.userId = :userId AND s.status = 'OPENED'")
  java.util.List<SalesSessionJpaEntity> findOpenByCashier(
      @Param("userId") UUID userId);

  // Find by tenant + outlet + status
  List<SalesSessionJpaEntity> findByTenantIdAndOutletIdAndStatus(
      UUID tenantId,
      UUID outletId,
      com.tchalanet.server.core.session.domain.model.SalesSessionStatus status);

  // Find by outlet + status (RLS version)
  List<SalesSessionJpaEntity> findByOutletIdAndStatus(
      UUID outletId,
      com.tchalanet.server.core.session.domain.model.SalesSessionStatus status);

  // Find session ids by tenant + outlet + openedAt between
  @Query(
      "SELECT s.id FROM SalesSessionJpaEntity s WHERE s.tenantId = :tenantId AND s.outletId = :outletId AND s.openedAt >= :from AND s.openedAt < :to")
  List<UUID> findIdsByTenantIdAndOutletIdAndOpenedAtBetween(
      @Param("tenantId") UUID tenantId,
      @Param("outletId") UUID outletId,
      @Param("from") Instant from,
      @Param("to") Instant to);

  // Find session ids by outlet + openedAt between (RLS version)
  @Query(
      "SELECT s.id FROM SalesSessionJpaEntity s WHERE s.outletId = :outletId AND s.openedAt >= :from AND s.openedAt < :to")
  List<UUID> findIdsByOutletIdAndOpenedAtBetween(
      @Param("outletId") UUID outletId,
      @Param("from") Instant from,
      @Param("to") Instant to);
}
