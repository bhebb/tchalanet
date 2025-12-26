package com.tchalanet.server.common.types.money;

import java.math.BigDecimal;

/** Value object for monetary amounts. */
public record Money(BigDecimal amount, CurrencyCode currency) {

  public Money {
    if (amount == null) throw new IllegalArgumentException("Money.amount is null");
    if (currency == null) throw new IllegalArgumentException("Money.currency is null");
  }

  /**
   * Static factory for Money.
   */
  public static Money of(BigDecimal amount, CurrencyCode currency) {
    return new Money(amount, currency);
  }
}
