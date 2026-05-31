package com.tchalanet.server.core.terminal.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalBindingId;
import com.tchalanet.server.core.terminal.api.query.TerminalProofPurpose;
import com.tchalanet.server.core.terminal.internal.application.port.out.nonce.TerminalDeviceNonceWriterPort;
import com.tchalanet.server.core.terminal.internal.infra.persistence.TerminalDeviceNonceJpaEntity;
import com.tchalanet.server.core.terminal.internal.infra.persistence.TerminalDeviceNonceJpaRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class JpaTerminalDeviceNonceAdapter implements TerminalDeviceNonceWriterPort {

    private final TerminalDeviceNonceJpaRepository repository;
    private final IdGenerator idGenerator;

    /**
     * Runs in its own isolated transaction (REQUIRES_NEW) so that a concurrent duplicate
     * insert triggers the DB unique constraint, rolls back only this sub-transaction,
     * and leaves the caller's outer transaction intact.
     * The existsBy pre-check was removed: it creates a TOCTOU window and causes the same
     * rollback-poisoning problem when the constraint fires between the check and the insert.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean checkAndRecord(
        TenantId tenantId,
        TerminalBindingId bindingId,
        TerminalProofPurpose purpose,
        String nonce,
        Instant signedAt,
        Instant expiresAt
    ) {
        var entity = new TerminalDeviceNonceJpaEntity();
        entity.setId(idGenerator.newUuid());
        entity.setTenantId(tenantId.value());
        entity.setBindingId(bindingId.value());
        entity.setPurpose(purpose);
        entity.setNonce(nonce);
        entity.setSignedAt(signedAt);
        entity.setExpiresAt(expiresAt);

        try {
            repository.saveAndFlush(entity);
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }
}
