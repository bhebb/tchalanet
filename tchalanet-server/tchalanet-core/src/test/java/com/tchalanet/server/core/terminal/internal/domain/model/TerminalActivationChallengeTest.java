package com.tchalanet.server.core.terminal.internal.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalActivationChallengeId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalActivationChallenge;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalChallengeChannel;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalChallengeStatus;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalChallengeType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerminalActivationChallengeTest {

  @Test
  void verifyHashConsumesChallengeOnce() {
    var challenge = challenge();

    var result = challenge.verifyHash("hash-123", Instant.parse("2026-05-26T10:01:00Z"));

    assertThat(result.verified()).isTrue();
    assertThat(result.challenge().status()).isEqualTo(TerminalChallengeStatus.CONSUMED);
    assertThat(result.challenge().attemptCount()).isEqualTo(1);
    assertThat(result.challenge().consumedAt()).isEqualTo(Instant.parse("2026-05-26T10:01:00Z"));

    var replay = result.challenge().verifyHash("hash-123", Instant.parse("2026-05-26T10:02:00Z"));

    assertThat(replay.verified()).isFalse();
    assertThat(replay.challenge().status()).isEqualTo(TerminalChallengeStatus.CONSUMED);
  }

  @Test
  void wrongHashCancelsAtMaxAttempts() {
    var challenge = challenge();

    var first = challenge.verifyHash("wrong", Instant.parse("2026-05-26T10:01:00Z"));
    var second = first.challenge().verifyHash("wrong", Instant.parse("2026-05-26T10:02:00Z"));
    var third = second.challenge().verifyHash("wrong", Instant.parse("2026-05-26T10:03:00Z"));

    assertThat(third.verified()).isFalse();
    assertThat(third.challenge().status()).isEqualTo(TerminalChallengeStatus.CANCELLED);
    assertThat(third.challenge().attemptCount()).isEqualTo(3);
  }

  @Test
  void expiredChallengeCannotBeVerified() {
    var challenge = challenge();

    var result = challenge.verifyHash("hash-123", Instant.parse("2026-05-26T10:20:00Z"));

    assertThat(result.verified()).isFalse();
    assertThat(result.challenge().status()).isEqualTo(TerminalChallengeStatus.EXPIRED);
    assertThat(result.challenge().attemptCount()).isZero();
  }

  private static TerminalActivationChallenge challenge() {
    return TerminalActivationChallenge.pending(
        TerminalActivationChallengeId.of(UUID.fromString("00000000-0000-0000-0000-000000000010")),
        TenantId.of(UUID.fromString("00000000-0000-0000-0000-000000000001")),
        TerminalId.of(UUID.fromString("00000000-0000-0000-0000-000000000002")),
        UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000003")),
        TerminalChallengeType.POS_PAIRING,
        TerminalChallengeChannel.QR,
        "hash-123",
        Instant.parse("2026-05-26T10:00:00Z"),
        Instant.parse("2026-05-26T10:15:00Z"),
        3
    );
  }
}
