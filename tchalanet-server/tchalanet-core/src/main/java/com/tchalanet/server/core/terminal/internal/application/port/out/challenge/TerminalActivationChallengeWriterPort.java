package com.tchalanet.server.core.terminal.internal.application.port.out.challenge;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalActivationChallenge;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalChallengeType;

public interface TerminalActivationChallengeWriterPort {

    TerminalActivationChallenge save(TerminalActivationChallenge challenge);

    /**
     * Cancel all PENDING challenges for the given terminal/user/type combination.
     * Called before creating a new challenge to avoid violating the unique constraint
     * {@code ux_terminal_challenge__pending_terminal_user_type}.
     */
    void revokeAllPending(TenantId tenantId, TerminalId terminalId, UserId userId, TerminalChallengeType challengeType);
}
