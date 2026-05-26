package com.tchalanet.server.core.terminal.internal.application.service.challenge;

import com.tchalanet.server.common.crypto.Hashing;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalActivationChallengeId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import java.util.Objects;

public final class TerminalChallengeCodeHasher {

    private TerminalChallengeCodeHasher() {}

    public static String hash(
        TenantId tenantId,
        TerminalId terminalId,
        UserId userId,
        TerminalActivationChallengeId challengeId,
        String clearCode
    ) {
        Objects.requireNonNull(tenantId, "tenantId is required");
        Objects.requireNonNull(terminalId, "terminalId is required");
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(challengeId, "challengeId is required");
        if (clearCode == null || clearCode.isBlank()) {
            throw new IllegalArgumentException("clearCode is required");
        }

        return Hashing.sha256Hex(
            tenantId.value() + "|" + terminalId.value() + "|" + userId.value()
                + "|" + challengeId.value() + "|" + clearCode.trim()
        );
    }
}
