package com.tchalanet.server.core.terminal.internal.domain.model.challenge;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalActivationChallengeId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;
import java.util.Objects;

public record TerminalActivationChallenge(
    TerminalActivationChallengeId id,
    TenantId tenantId,
    TerminalId terminalId,
    UserId userId,
    TerminalChallengeType challengeType,
    TerminalChallengeChannel channel,
    String codeHash,
    Instant expiresAt,
    int attemptCount,
    int maxAttempts,
    TerminalChallengeStatus status,
    Instant createdAt,
    Instant consumedAt
) {

    public TerminalActivationChallenge {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(tenantId, "tenantId is required");
        Objects.requireNonNull(terminalId, "terminalId is required");
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(challengeType, "challengeType is required");
        Objects.requireNonNull(channel, "channel is required");
        if (codeHash == null || codeHash.isBlank()) {
            throw new IllegalArgumentException("codeHash is required");
        }
        Objects.requireNonNull(expiresAt, "expiresAt is required");
        Objects.requireNonNull(status, "status is required");
        Objects.requireNonNull(createdAt, "createdAt is required");
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("maxAttempts must be positive");
        }
        if (attemptCount < 0 || attemptCount > maxAttempts) {
            throw new IllegalArgumentException("attemptCount must be within maxAttempts");
        }
        if (status == TerminalChallengeStatus.CONSUMED && consumedAt == null) {
            throw new IllegalArgumentException("consumedAt is required for consumed challenge");
        }
    }

    public static TerminalActivationChallenge pending(
        TerminalActivationChallengeId id,
        TenantId tenantId,
        TerminalId terminalId,
        UserId userId,
        TerminalChallengeType challengeType,
        TerminalChallengeChannel channel,
        String codeHash,
        Instant createdAt,
        Instant expiresAt,
        int maxAttempts
    ) {
        return new TerminalActivationChallenge(
            id,
            tenantId,
            terminalId,
            userId,
            challengeType,
            channel,
            codeHash,
            expiresAt,
            0,
            maxAttempts,
            TerminalChallengeStatus.PENDING,
            createdAt,
            null
        );
    }

    public TerminalActivationChallenge expireIfDue(Instant now) {
        if (status != TerminalChallengeStatus.PENDING || expiresAt.isAfter(now)) {
            return this;
        }
        return copy(attemptCount, TerminalChallengeStatus.EXPIRED, consumedAt);
    }

    public VerificationResult verifyHash(String candidateHash, Instant now) {
        var current = expireIfDue(now);
        if (current.status != TerminalChallengeStatus.PENDING) {
            return new VerificationResult(current, false);
        }

        var nextAttemptCount = current.attemptCount + 1;
        if (!current.codeHash.equals(candidateHash)) {
            var failedStatus =
                nextAttemptCount >= current.maxAttempts ? TerminalChallengeStatus.CANCELLED : TerminalChallengeStatus.PENDING;
            return new VerificationResult(current.copy(nextAttemptCount, failedStatus, null), false);
        }

        return new VerificationResult(
            current.copy(nextAttemptCount, TerminalChallengeStatus.CONSUMED, now),
            true
        );
    }

    private TerminalActivationChallenge copy(
        int nextAttemptCount,
        TerminalChallengeStatus nextStatus,
        Instant nextConsumedAt
    ) {
        return new TerminalActivationChallenge(
            id,
            tenantId,
            terminalId,
            userId,
            challengeType,
            channel,
            codeHash,
            expiresAt,
            nextAttemptCount,
            maxAttempts,
            nextStatus,
            createdAt,
            nextConsumedAt
        );
    }

    public record VerificationResult(
        TerminalActivationChallenge challenge,
        boolean verified
    ) {}
}
