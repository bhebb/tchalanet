package com.tchalanet.server.core.terminal.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TerminalJpaRepository
    extends JpaRepository<TerminalJpaEntity, UUID>, JpaSpecificationExecutor<TerminalJpaEntity> {

    Optional<TerminalJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    List<TerminalJpaEntity> findByOutletId(UUID outletId);

    long countByOutletId(UUID outletId);

    Optional<TerminalJpaEntity> findFirstByAssignedUserIdAndAutoSessionEnabledIsTrue(UUID assignedUserId);

    List<TerminalJpaEntity> findBySyncState(String syncState);
}
