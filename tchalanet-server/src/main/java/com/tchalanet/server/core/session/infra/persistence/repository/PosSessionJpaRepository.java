package com.tchalanet.server.core.session.infra.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import com.tchalanet.server.core.session.infra.persistence.entity.PosSessionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PosSessionJpaRepository extends JpaRepository<PosSessionJpaEntity, UUID> {

    /**
     * Finds an open POS session for the given tenant and terminal.
     */
    Optional<PosSessionJpaEntity> findByTenantIdAndTerminalIdAndStatus(
        @Param("tenantId") UUID tenantId,
        @Param("terminalId") UUID terminalId,
        @Param("status") String status);

    /**
     * Finds all open sessions for a cashier.
     */
    @Query("SELECT s FROM PosSessionJpaEntity s WHERE s.tenantId = :tenantId AND s.userId = :userId AND s.status = 'OPEN'")
    java.util.List<PosSessionJpaEntity> findOpenByCashier(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId);
}
