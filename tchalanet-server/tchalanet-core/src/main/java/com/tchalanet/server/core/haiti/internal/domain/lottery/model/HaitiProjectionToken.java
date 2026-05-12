package com.tchalanet.server.core.haiti.internal.domain.lottery.model;

/** Tokens describing how to project external picks into Haiti lots */
public enum HaitiProjectionToken {
  PICK3_FULL_3,
  PICK3_FIRST2,
  PICK3_LAST2,
  PICK4_FULL_4,
  PICK4_FIRST2,
  PICK4_LAST2;

  public static HaitiProjectionToken parse(String raw) {
    if (raw == null || raw.isBlank())
      throw new IllegalArgumentException("projection token is blank");
    return HaitiProjectionToken.valueOf(raw.trim().toUpperCase());
  }
}
