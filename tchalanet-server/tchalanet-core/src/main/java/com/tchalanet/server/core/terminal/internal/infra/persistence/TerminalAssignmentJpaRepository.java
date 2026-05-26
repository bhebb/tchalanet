package com.tchalanet.server.core.terminal.internal.infra.persistence;

import com.tchalanet.server.common.persistence.repository.TchJpaRepository;
import com.tchalanet.server.core.terminal.internal.domain.model.assignment.TerminalAssignmentStatus;
import java.util.Optional;
import java.util.UUID;

public interface TerminalAssignmentJpaRepository extends TchJpaRepository<TerminalAssignmentJpaEntity, UUID> {

    Optional<TerminalAssignmentJpaEntity> findByTenantIdAndTerminalIdAndUserIdAndStatus(
        UUID tenantId,
        UUID terminalId,
        UUID userId,
        TerminalAssignmentStatus status
    );
}
