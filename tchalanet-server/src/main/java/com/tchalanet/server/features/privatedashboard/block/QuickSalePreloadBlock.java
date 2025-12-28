package com.tchalanet.server.features.privatedashboard.block;

import java.util.List;

public record QuickSalePreloadBlock(boolean enabled, List<String> productSkus, int preloadCount) {
  public static QuickSalePreloadBlock empty() {
    return new QuickSalePreloadBlock(false, List.of(), 0);
  }
}
