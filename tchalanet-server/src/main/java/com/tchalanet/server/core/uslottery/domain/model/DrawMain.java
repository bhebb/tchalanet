package com.tchalanet.server.core.uslottery.domain.model;

import java.util.List;

public record DrawMain(List<String> ordered) {

  public DrawMain {
    if (ordered == null) throw new IllegalArgumentException("main numbers required");

    var cleaned =
        ordered.stream().map(s -> s == null ? "" : s.trim()).filter(s -> !s.isBlank()).toList();

    if (cleaned.isEmpty()) throw new IllegalArgumentException("main numbers empty");

    for (String s : cleaned) {
      // accept multi-digit numbers (e.g. "24"), but ensure all characters are digits
      if (!s.matches("\\d+")) {
        throw new IllegalArgumentException("invalid digit: " + s);
      }
    }

    ordered = List.copyOf(cleaned);
  }

  public int size() {
    return ordered.size();
  }

  public DrawMain requireSize(int expected, String context) {
    if (ordered.size() != expected) {
      throw new IllegalArgumentException(
          "invalid numbers count for "
              + context
              + ": expected "
              + expected
              + ", got "
              + ordered.size());
    }
    return this;
  }
}
