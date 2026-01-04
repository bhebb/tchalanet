package com.tchalanet.server.core.session.infra.persistence.repository;

import com.tchalanet.server.core.session.infra.persistence.entity.PosSessionJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PosSessionJpaRepository extends JpaRepository<PosSessionJpaEntity, UUID> {

  /** Finds an open POS session for the given tenant and terminal. */
  Optional<PosSessionJpaEntity> findByTenantIdAndTerminalIdAndStatus(
      @Param("tenantId") UUID tenantId,
      @Param("terminalId") UUID terminalId,
      @Param("status") com.tchalanet.server.core.session.domain.model.PosSessionStatus status);

  /** Finds all open sessions for a cashier. */
  @Query(
      "SELECT s FROM PosSessionJpaEntity s WHERE s.tenantId = :tenantId AND s.userId = :userId AND s.status = 'OPENED'")
  java.util.List<PosSessionJpaEntity> findOpenByCashier(
      @Param("tenantId") UUID tenantId, @Param("userId") UUID userId);

  // Find by tenant + outlet + status
  List<PosSessionJpaEntity> findByTenantIdAndOutletIdAndStatus(
      UUID tenantId,
      UUID outletId,
      com.tchalanet.server.core.session.domain.model.PosSessionStatus status);

  // Find session ids by tenant + outlet + openedAt between
  @Query(
      "SELECT s.id FROM PosSessionJpaEntity s WHERE s.tenantId = :tenantId AND s.outletId = :outletId AND s.openedAt >= :from AND s.openedAt < :to")
  List<UUID> findIdsByTenantIdAndOutletIdAndOpenedAtBetween(
      @Param("tenantId") UUID tenantId,
      @Param("outletId") UUID outletId,
      @Param("from") Instant from,
      @Param("to") Instant to);
}
