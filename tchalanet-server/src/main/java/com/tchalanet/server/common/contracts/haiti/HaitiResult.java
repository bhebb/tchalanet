package com.tchalanet.server.common.contracts.haiti;

import com.tchalanet.server.core.haiti.domain.lottery.model.HaitiLot;
import java.util.Map;
import java.util.Objects;

public record HaitiResult(Map<HaitiLot, String> lots) {
  public HaitiResult {
    Objects.requireNonNull(lots, "lots");
    for (HaitiLot lot : HaitiLot.values())
      if (!lots.containsKey(lot))
        throw new IllegalArgumentException("Missing lot value for " + lot);
    for (Map.Entry<HaitiLot, String> e : lots.entrySet()) {
      if (e.getValue() == null || e.getValue().isBlank())
        throw new IllegalArgumentException("Lot value must be non-blank for " + e.getKey());
    }
    lots = Map.copyOf(lots);
  }

  public String lot(HaitiLot lot) {
    return lots.get(lot);
  }
}
