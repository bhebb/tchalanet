package com.tchalanet.server.core.terminal.api.command;

import com.tchalanet.server.common.types.id.TerminalActivationChallengeId;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalChallengeChannel;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalChallengeType;
import java.time.Instant;

public record CreateTerminalActivationChallengeResult(
    TerminalActivationChallengeId challengeId,
    TerminalChallengeType challengeType,
    TerminalChallengeChannel channel,
    Instant expiresAt,
    String deliveryRef
) {}
