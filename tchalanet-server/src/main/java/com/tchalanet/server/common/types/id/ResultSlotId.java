package com.tchalanet.server.common.types.id;

import java.util.UUID;

public final class ResultSlotId {
  private final UUID value;

  private ResultSlotId(UUID value) {
    this.value = value;
  }

  public static ResultSlotId of(UUID u) {
    return u == null ? null : new ResultSlotId(u);
  }

  public UUID uuid() {
    return value;
  }

  @Override
  public String toString() {
    return value == null ? null : value.toString();
  }
}
