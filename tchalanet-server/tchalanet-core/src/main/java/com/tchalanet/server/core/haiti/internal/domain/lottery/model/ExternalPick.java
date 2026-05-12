package com.tchalanet.server.core.haiti.internal.domain.lottery.model;

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

  private static String normalize(String value) {
    if (value == null) return null;
    String trimmed = value.trim().replaceAll("\\s+", "");
    if (trimmed.isEmpty()) return null;
    if (!trimmed.chars().allMatch(Character::isDigit))
      throw new InvalidExternalPickException("picks must contain digits only");
    return trimmed;
  }
}
