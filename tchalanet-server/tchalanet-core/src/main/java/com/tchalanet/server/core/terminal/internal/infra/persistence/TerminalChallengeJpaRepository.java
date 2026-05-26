package com.tchalanet.server.core.terminal.internal.infra.persistence;

import com.tchalanet.server.common.persistence.repository.TchJpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface TerminalChallengeJpaRepository extends TchJpaRepository<TerminalChallengeJpaEntity, UUID> {

    Optional<TerminalChallengeJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);
}
