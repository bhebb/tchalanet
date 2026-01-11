package com.tchalanet.server.common.types.codes;

/** Value object for selection keys. */
public record SelectionKey(String key) {

  public SelectionKey {
    if (key == null || key.isBlank()) {
      throw new IllegalArgumentException("SelectionKey.slotKey is null or blank");
    }
  }

  /** Static factory for SelectionKey. */
  public static SelectionKey of(String key) {
    return new SelectionKey(key);
  }

  @Override
  public String toString() {
    return key;
  }
}
