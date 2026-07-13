package com.tchalanet.server.core.sales.api.query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TenantTopSelectionsByPeriodView(
    LocalDate from,
    LocalDate to,
    List<SelectionItem> topSelections
) {
  public record SelectionItem(
      int rank,
      String displaySelection,
      String gameCode,
      String betType,
      Short betOption,
      long lineCount,
      BigDecimal totalStake
  ) {}
}
