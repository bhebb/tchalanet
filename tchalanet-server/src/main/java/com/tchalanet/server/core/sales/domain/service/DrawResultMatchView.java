package com.tchalanet.server.core.sales.domain.service;

import java.util.Set;

public interface DrawResultMatchView {

  // Haiti lots (2D)
  String lot1(); // "00".."99" (nullable si pas dispo)
  String lot2();
  String lot3();

  // Lotto5 uses pick3 (si applicable)
  String pick3(); // "000".."999" (nullable)

  default Set<String> twoDigits() {
    return Set.of(
        lot1() == null ? "" : lot1(),
        lot2() == null ? "" : lot2(),
        lot3() == null ? "" : lot3()
    ).stream().filter(s -> s != null && !s.isBlank()).collect(java.util.stream.Collectors.toSet());
  }
}

