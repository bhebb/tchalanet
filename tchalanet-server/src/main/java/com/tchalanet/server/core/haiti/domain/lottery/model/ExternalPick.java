package com.tchalanet.server.core.haiti.domain.lottery.model;

import com.tchalanet.server.core.haiti.domain.lottery.exception.InvalidExternalPickException;

public record ExternalPick(String pick3, String pick4) {
  public static ExternalPick of(String p3, String p4) {
    String n3 = normalize(p3);
    String n4 = normalize(p4);
    if (n3 == null || n3.length() != 3)
      throw new InvalidExternalPickException("pick3 must be 3 digits");
    if (n4 == null || n4.length() != 4)
      throw new InvalidExternalPickException("pick4 must be 4 digits");
    return new ExternalPick(n3, n4);
  }

  private static String normalize(String s) {
    if (s == null) return null;
    String t = s.trim().replaceAll("\\s+", "");
    if (t.isEmpty()) return null;
    if (!t.chars().allMatch(Character::isDigit))
      throw new InvalidExternalPickException("picks must contain digits only");
    return t;
  }
}
