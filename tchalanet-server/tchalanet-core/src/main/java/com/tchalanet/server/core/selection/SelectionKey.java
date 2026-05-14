package com.tchalanet.server.core.selection;

/** Value object for canonical selection keys. */
public record SelectionKey(String key) {

  public SelectionKey {
    if (key == null || key.isBlank()) {
      throw new IllegalArgumentException("SelectionKey.key is null or blank");
    }
  }

  public static SelectionKey of(String key) {
    return new SelectionKey(key);
  }

  @Override
  public String toString() {
    return key;
  }
}
