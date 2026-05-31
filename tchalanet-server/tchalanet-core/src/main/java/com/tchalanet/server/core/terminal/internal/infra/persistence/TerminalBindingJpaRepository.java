package com.tchalanet.server.core.terminal.internal.infra.persistence;

import com.tchalanet.server.common.persistence.repository.TchJpaRepository;
import com.tchalanet.server.core.terminal.internal.domain.model.binding.TerminalBindingStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TerminalBindingJpaRepository extends TchJpaRepository<TerminalBindingJpaEntity, UUID> {

    List<TerminalBindingJpaEntity> findByTenantIdAndTerminalIdAndStatus(
        UUID tenantId,
        UUID terminalId,
        TerminalBindingStatus status
    );

    Optional<TerminalBindingJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);
}
