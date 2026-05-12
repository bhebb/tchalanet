package com.tchalanet.server.common.types.money;

/** Value object for currency codes. */
public record CurrencyCode(String code) {

  public CurrencyCode {
    if (code == null || code.isBlank())
      throw new IllegalArgumentException("CurrencyCode.code is null or blank");
  }

  /** Static factory for CurrencyCode. */
  public static CurrencyCode of(String code) {
    return new CurrencyCode(code);
  }
}
