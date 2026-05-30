package com.tchalanet.server.catalog.game.api.model;

import java.util.List;

public enum BetType {
  MATCH_1_2D(2),
  MATCH_2_2D(2),
  MATCH_3_2D(2),
  LOTTO3_3D(3),
  MARRIAGE_2D2D(2),
  LOTTO4_PATTERN(4),
  LOTTO5_PATTERN(5);

  private final int width;

  BetType(int width) {
    this.width = width;
  }

  public int canonicalWidth() {
    return width;
  }

  public boolean requiresOption() {
    return BetOption.requiresOption(this);
  }

  public List<BetOption> allowedOptions() {
    return BetOption.allowedFor(this);
  }

  public boolean supportsOption(Short option) {
    try {
      BetOption.from(this, option);
      return true;
    } catch (IllegalArgumentException ex) {
      return false;
    }
  }

  public boolean requiresBetOption() {
    return requiresOption();
  }

  public short betOptionMin() {
    return allowedOptions().stream()
        .map(BetOption::code)
        .min(Short::compare)
        .orElse((short) 0);
  }

  public short betOptionMax() {
    return allowedOptions().stream()
        .map(BetOption::code)
        .max(Short::compare)
        .orElse((short) 0);
  }

  public boolean isPattern() {
    return this == LOTTO4_PATTERN || this == LOTTO5_PATTERN;
  }

  public boolean isBorlette() {
    return this == MATCH_1_2D || this == MATCH_2_2D || this == MATCH_3_2D;
  }
}
