package com.tchalanet.server.core.terminal.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalChallengeChannel;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalChallengeStatus;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalChallengeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "terminal_challenge")
@Getter
@Setter
public class TerminalChallengeJpaEntity extends BaseTenantEntity {

    @Column(name = "terminal_id", nullable = false)
    private UUID terminalId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "challenge_type", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private TerminalChallengeType challengeType;

    @Column(name = "channel", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private TerminalChallengeChannel channel;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "status", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private TerminalChallengeStatus status;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;
}
