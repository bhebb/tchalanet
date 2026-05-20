package com.tchalanet.server.core.haiti.api;

import java.util.Objects;

/**
 * Result of a Haiti projection: either a successful HaitiResult with flags.projectionOk=true or a
 * failure where result is null and flags.projectionOk=false.
 */
public record HaitiProjectionOutput(HaitiResult result, HaitiFlags flags) {

  public HaitiProjectionOutput {
    Objects.requireNonNull(flags, "flags required");
    // result may be null when projectionOk is false
  }

  public static HaitiProjectionOutput ok(HaitiResult result, HaitiFlags flags) {
    Objects.requireNonNull(result, "result required");
    Objects.requireNonNull(flags, "flags required");
    if (!flags.projectionOk()) throw new IllegalArgumentException("flags must be ok");
    return new HaitiProjectionOutput(result, flags);
  }

  public static HaitiProjectionOutput fail(HaitiFlags flags) {
    Objects.requireNonNull(flags, "flags required");
    if (flags.projectionOk()) throw new IllegalArgumentException("flags must be fail");
    return new HaitiProjectionOutput(null, flags);
  }
}
