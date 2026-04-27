package com.tchalanet.server.common.types.money;

import java.math.BigDecimal;

/** Value object for percentages. */
public record Percent(BigDecimal value) {

  public Percent {
    if (value == null) throw new IllegalArgumentException("Percent.value is null");
  }

  /** Static factory for Percent. */
  public static Percent of(BigDecimal value) {
    return new Percent(value);
  }
}
