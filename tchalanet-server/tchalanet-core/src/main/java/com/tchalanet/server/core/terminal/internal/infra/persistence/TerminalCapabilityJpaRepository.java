package com.tchalanet.server.core.terminal.internal.infra.persistence;

import com.tchalanet.server.common.persistence.repository.TchJpaRepository;
import java.util.List;
import java.util.UUID;

public interface TerminalCapabilityJpaRepository extends TchJpaRepository<TerminalCapabilityJpaEntity, UUID> {

    List<TerminalCapabilityJpaEntity> findByTenantIdAndTerminalId(UUID tenantId, UUID terminalId);
}
