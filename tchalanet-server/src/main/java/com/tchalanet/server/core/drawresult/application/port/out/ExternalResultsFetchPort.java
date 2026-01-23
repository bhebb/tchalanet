package com.tchalanet.server.core.drawresult.application.port.out;

import com.tchalanet.server.common.contracts.results.ExternalResultOutput;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

public interface ExternalResultsFetchPort {

  // --- new slot-first API (recommended) ---
  ExternalBundle fetchSlot(ResultSlotFetchQuery q);

  record ResultSlotFetchQuery(
      String slotKey, LocalDate date, boolean force, boolean dryRun, Instant now) {
    public ResultSlotFetchQuery {
      Objects.requireNonNull(slotKey, "slotKey required");
      if (date == null) throw new IllegalArgumentException("date required");
      if (now == null) throw new IllegalArgumentException("now required");
    }
  }

  record ExternalBundle(
      ExternalResultOutput pick3, ExternalResultOutput pick4, Map<String, Object> raw) {}
}
