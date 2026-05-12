package com.tchalanet.server.common.contracts.haiti;

import java.time.Instant;
import java.util.Map;

public record HaitiFlags(
    int projectionVersion,
    boolean success,
    String reason,
    String ruleSet,
    Instant projectedAt,
    Map<String, ?> details) {

  public static HaitiFlags fail(int projectionVersion, String reason, Map<String, ?> details) {
    return fail(projectionVersion, "DEFAULT", reason, Instant.now(), details);
  }

  public static HaitiFlags fail(
      int projectionVersion,
      String ruleSet,
      String reason,
      Instant projectedAt,
      Map<String, ?> details) {
    return new HaitiFlags(projectionVersion, false, reason, ruleSet, projectedAt, details);
  }
}
