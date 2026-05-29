package com.tchalanet.server.common.types.id;

import java.util.UUID;

public record TerminalAssignmentId(UUID value) {
  public TerminalAssignmentId {
    if (value == null) {
      throw new IllegalArgumentException("TerminalAssignmentId.value is null");
    }
  }

  public static TerminalAssignmentId of(UUID value) {
    return new TerminalAssignmentId(value);
  }

  public static TerminalAssignmentId nullableOf(UUID raw) {
    return raw == null ? null : new TerminalAssignmentId(raw);
  }

  public static TerminalAssignmentId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("TerminalAssignmentId string is required");
    }
    return new TerminalAssignmentId(UUID.fromString(raw));
  }
}
