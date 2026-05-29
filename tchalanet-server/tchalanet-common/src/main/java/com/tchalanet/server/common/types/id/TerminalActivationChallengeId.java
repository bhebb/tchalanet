package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record TerminalActivationChallengeId(UUID value) {
  public TerminalActivationChallengeId {
    if (value == null) {
      throw new IllegalArgumentException("TerminalActivationChallengeId.value is null");
    }
  }

  public static TerminalActivationChallengeId of(UUID value) {
    return new TerminalActivationChallengeId(value);
  }

  public static TerminalActivationChallengeId nullableOf(UUID raw) {
    return raw == null ? null : new TerminalActivationChallengeId(raw);
  }

  public static TerminalActivationChallengeId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("TerminalActivationChallengeId string is required");
    }
    return new TerminalActivationChallengeId(UUID.fromString(raw));
  }
}
