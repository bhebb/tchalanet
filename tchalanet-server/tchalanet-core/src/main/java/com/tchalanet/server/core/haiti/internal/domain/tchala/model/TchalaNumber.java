package com.tchalanet.server.core.haiti.internal.domain.tchala.model;

import com.tchalanet.server.core.haiti.domain.tchala.exception.InvalidTchalaNumberException;

public record TchalaNumber(int value) {
  public static TchalaNumber of(int v) {
    if (v < 0 || v > 99) throw new InvalidTchalaNumberException("Tchala number out of range: " + v);
    return new TchalaNumber(v);
  }

  public String asTwoDigits() {
    return String.format("%02d", value);
  }
}
