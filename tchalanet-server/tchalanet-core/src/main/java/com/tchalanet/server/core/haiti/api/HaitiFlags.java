package com.tchalanet.server.core.haiti.api;

import java.time.Instant;
import java.util.Map;

public record HaitiFlags(
    int projectionVersion,
    boolean projectionOk,
    String projectionReason,
    String ruleSet,
    Instant computedAt,
    Map<String, Object> extra) {

  public HaitiFlags {
    if (projectionVersion < 0) projectionVersion = 0;
    if (projectionReason == null) projectionReason = "";
    if (ruleSet == null || ruleSet.isBlank()) ruleSet = "DEFAULT";
    if (computedAt == null) computedAt = Instant.EPOCH;
    extra = (extra == null) ? Map.of() : Map.copyOf(extra);
  }

  public static HaitiFlags ok(int version, Instant computedAt) {
    return new HaitiFlags(version, true, "", "DEFAULT", computedAt, Map.of());
  }

  public static HaitiFlags ok(int version) {
    return ok(version, Instant.now());
  }

  public static HaitiFlags fail(
      int version, String ruleSet, String reason, Instant computedAt, Map<String, Object> extra) {
    return new HaitiFlags(version, false, reason, ruleSet, computedAt, extra);
  }

  public static HaitiFlags fail(int version, String reason, Map<String, Object> extra) {
    return new HaitiFlags(
        version,
        false,
        reason == null ? "ERROR" : reason,
        "DEFAULT",
        Instant.now(),
        extra == null ? Map.of() : Map.copyOf(extra));
  }
}
