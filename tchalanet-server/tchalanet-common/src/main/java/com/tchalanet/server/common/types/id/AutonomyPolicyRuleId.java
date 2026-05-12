package com.tchalanet.server.common.types.id;

import java.util.UUID;

/** Typed identifier for an Autonomy policy rule row. */
public record AutonomyPolicyRuleId(UUID value) {

  public AutonomyPolicyRuleId {
    if (value == null) throw new IllegalArgumentException("AutonomyPolicyRuleId.value is null");
  }

  public static AutonomyPolicyRuleId of(UUID value) {
    return new AutonomyPolicyRuleId(value);
  }

  public static AutonomyPolicyRuleId nullableOf(UUID raw) {
    return raw == null ? null : new AutonomyPolicyRuleId(raw);
  }

  public static AutonomyPolicyRuleId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("AutonomyPolicyRuleId string is required");
    }
    return new AutonomyPolicyRuleId(UUID.fromString(raw));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
