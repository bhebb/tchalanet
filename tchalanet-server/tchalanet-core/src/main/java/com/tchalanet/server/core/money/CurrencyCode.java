package com.tchalanet.server.core.money;

import java.util.Currency;
import java.util.Locale;

/** ISO 4217 currency code used by core money-bearing domains. */
public record CurrencyCode(String value) {

  public CurrencyCode {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("currency code is required");
    }
    value = value.trim().toUpperCase(Locale.ROOT);
    Currency.getInstance(value);
  }

  public static CurrencyCode of(String value) {
    return new CurrencyCode(value);
  }

  public String code() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }
}
