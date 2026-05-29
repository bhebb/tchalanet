package com.tchalanet.server.core.terminal.internal.domain.model.challenge;

import com.tchalanet.server.common.types.id.TerminalActivationChallengeId;
import java.time.Instant;
import java.util.Objects;

public record TerminalChallengeDelivery(
    TerminalActivationChallengeId challengeId,
    TerminalChallengeType type,
    TerminalChallengeChannel channel,
    String deliveryRef,
    Instant deliveredAt
) {

    public TerminalChallengeDelivery {
        Objects.requireNonNull(challengeId, "challengeId is required");
        Objects.requireNonNull(type, "type is required");
        Objects.requireNonNull(channel, "channel is required");
        Objects.requireNonNull(deliveredAt, "deliveredAt is required");
    }
}
