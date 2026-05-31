package com.tchalanet.server.core.terminal.internal.application.service;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalBindingId;
import com.tchalanet.server.core.terminal.api.query.TerminalProofPurpose;
import com.tchalanet.server.core.terminal.internal.application.port.out.nonce.TerminalDeviceNonceWriterPort;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TerminalNonceReplayGuard {

    private static final Duration CLOCK_SKEW = Duration.ofMinutes(5);
    private static final Duration NONCE_TTL = Duration.ofMinutes(65);

    private final TerminalDeviceNonceWriterPort nonceWriter;

    /**
     * Returns true if the signedAt is within the clock skew window and the nonce has not been replayed.
     * Records the nonce on success.
     */
    public boolean validateAndRecord(
        TenantId tenantId,
        TerminalBindingId bindingId,
        TerminalProofPurpose purpose,
        String nonce,
        Instant signedAt,
        Instant now
    ) {
        var delta = Duration.between(signedAt, now).abs();
        if (delta.compareTo(CLOCK_SKEW) > 0) {
            return false;
        }

        var expiresAt = signedAt.plus(NONCE_TTL);
        return nonceWriter.checkAndRecord(tenantId, bindingId, purpose, nonce, signedAt, expiresAt);
    }
}
