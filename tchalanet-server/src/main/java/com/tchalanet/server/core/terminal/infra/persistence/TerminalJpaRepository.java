package com.tchalanet.server.core.terminal.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TerminalJpaRepository extends JpaRepository<TerminalJpaEntity, UUID> {

  Optional<TerminalJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

  @Query(
      "SELECT t FROM TerminalJpaEntity t WHERE t.tenantId = :tenantId AND t.outletId = :outletId AND t.deletedAt IS NULL")
  List<TerminalJpaEntity> findAllByTenantIdAndOutletIdAndDeletedAtIsNull(
      @Param("tenantId") UUID tenantId, @Param("outletId") UUID outletId, Pageable pageable);

  @Query("SELECT t FROM TerminalJpaEntity t WHERE t.tenantId = :tenantId AND t.deletedAt IS NULL")
  List<TerminalJpaEntity> findAllByTenantIdAndDeletedAtIsNull(
      @Param("tenantId") UUID tenantId, Pageable pageable);
}
