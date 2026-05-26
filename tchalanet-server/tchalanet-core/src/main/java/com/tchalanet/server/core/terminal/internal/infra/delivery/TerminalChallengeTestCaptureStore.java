package com.tchalanet.server.core.terminal.internal.infra.delivery;

import com.tchalanet.server.common.types.id.TerminalActivationChallengeId;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class TerminalChallengeTestCaptureStore {

    private final ConcurrentHashMap<TerminalActivationChallengeId, CapturedChallengeCode> captures =
        new ConcurrentHashMap<>();

    public void capture(TerminalActivationChallengeId challengeId, String clearCode, Instant capturedAt) {
        captures.put(challengeId, new CapturedChallengeCode(challengeId, clearCode, capturedAt));
    }

    public Optional<CapturedChallengeCode> find(TerminalActivationChallengeId challengeId) {
        return Optional.ofNullable(captures.get(challengeId));
    }

    public void remove(TerminalActivationChallengeId challengeId) {
        captures.remove(challengeId);
    }

    public record CapturedChallengeCode(
        TerminalActivationChallengeId challengeId,
        String clearCode,
        Instant capturedAt
    ) {}
}
