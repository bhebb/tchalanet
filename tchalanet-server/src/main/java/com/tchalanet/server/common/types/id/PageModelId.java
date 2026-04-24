package com.tchalanet.server.common.types.id;

import java.util.UUID;

public final class PageModelId {
  private final UUID value;
  private PageModelId(UUID value) { this.value = value; }
  public static PageModelId of(UUID v) { return new PageModelId(v); }
  public static PageModelId of(String s) { return new PageModelId(UUID.fromString(s)); }
  public UUID uuid() { return value; }
  @Override public String toString() { return value.toString(); }
}

