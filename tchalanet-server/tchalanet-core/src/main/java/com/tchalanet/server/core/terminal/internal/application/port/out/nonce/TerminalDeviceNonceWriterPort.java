package com.tchalanet.server.core.terminal.internal.application.port.out.nonce;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalBindingId;
import com.tchalanet.server.core.terminal.api.query.TerminalProofPurpose;
import java.time.Instant;

public interface TerminalDeviceNonceWriterPort {

    /**
     * Atomically attempts to record a nonce for replay detection.
     *
     * <p>Returns {@code true} if the nonce was successfully recorded (first use).
     * Returns {@code false} if the nonce already exists (replay detected).
     *
     * <p><strong>Atomicity contract:</strong> Implementations MUST run in an isolated
     * transaction ({@code REQUIRES_NEW} or equivalent) so that a unique-constraint
     * violation on a concurrent duplicate does not poison the caller's outer transaction.
     * The DB unique constraint on {@code (tenant_id, binding_id, purpose, nonce)} is the
     * authoritative replay guard — do not rely on a pre-check {@code SELECT} to avoid it.
     */
    boolean checkAndRecord(
        TenantId tenantId,
        TerminalBindingId bindingId,
        TerminalProofPurpose purpose,
        String nonce,
        Instant signedAt,
        Instant expiresAt
    );
}
