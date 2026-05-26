package com.tchalanet.server.core.terminal.internal.application.port.out.challenge;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalActivationChallengeId;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalActivationChallenge;
import java.util.Optional;

public interface TerminalActivationChallengeReaderPort {

    Optional<TerminalActivationChallenge> findById(
        TenantId tenantId,
        TerminalActivationChallengeId challengeId
    );

    default TerminalActivationChallenge getRequired(
        TenantId tenantId,
        TerminalActivationChallengeId challengeId
    ) {
        return findById(tenantId, challengeId)
            .orElseThrow(() -> new IllegalArgumentException("Terminal activation challenge not found: " + challengeId));
    }
}
