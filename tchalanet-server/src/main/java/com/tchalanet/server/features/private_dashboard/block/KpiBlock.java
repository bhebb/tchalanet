package com.tchalanet.server.features.private_dashboard.block;

import java.util.List;

public record KpiBlock(List<KpiItem> items) {
  public static KpiBlock empty() {
    return new KpiBlock(List.of());
  }

  public record KpiItem(String code, String labelKey, String formattedValue, String trend) {}
}
