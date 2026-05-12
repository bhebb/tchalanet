package com.tchalanet.server.common.types.codes;

/** Value object for locale codes. */
public record LocaleCode(String code) {

  public LocaleCode {
    if (code == null || code.isBlank())
      throw new IllegalArgumentException("LocaleCode.code is null or blank");
  }

  /** Static factory for LocaleCode. */
  public static LocaleCode of(String code) {
    return new LocaleCode(code);
  }
}
