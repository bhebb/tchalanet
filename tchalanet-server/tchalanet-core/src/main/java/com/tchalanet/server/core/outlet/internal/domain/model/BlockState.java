package com.tchalanet.server.core.outlet.internal.domain.model;

import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;

/**
 * Immutable value object representing a blocking state for a specific outlet capability
 * (outlet-global, sales, payout, offline-sales). Groups the 4 audit fields together.
 */
public record BlockState(
    boolean blocked,
    String reason,
    Instant at,
    UserId by) {

  public static BlockState none() {
    return new BlockState(false, null, null, null);
  }

  public BlockState block(String reason, Instant at, UserId by) {
    return new BlockState(true, reason, at, by);
  }

  public BlockState unblock() {
    return none();
  }
}
