package com.tchalanet.server.core.terminal.internal.infra.persistence;

import com.tchalanet.server.core.terminal.api.query.TerminalProofPurpose;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TerminalDeviceNonceJpaRepository extends JpaRepository<TerminalDeviceNonceJpaEntity, UUID> {

    boolean existsByTenantIdAndBindingIdAndPurposeAndNonce(
        UUID tenantId,
        UUID bindingId,
        TerminalProofPurpose purpose,
        String nonce
    );
}
