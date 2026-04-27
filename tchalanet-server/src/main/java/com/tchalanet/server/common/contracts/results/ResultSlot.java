package com.tchalanet.server.common.contracts.results;

import java.time.LocalDate;

/** Canonical slot: draw channel code + local draw date. */
public record ResultSlot(String channelCode, LocalDate drawDate) {
  public ResultSlot {
    if (channelCode == null || channelCode.isBlank())
      throw new IllegalArgumentException("slotKey required");
    if (drawDate == null) throw new IllegalArgumentException("drawDate required");
    channelCode = channelCode.trim().toUpperCase();
  }
}
