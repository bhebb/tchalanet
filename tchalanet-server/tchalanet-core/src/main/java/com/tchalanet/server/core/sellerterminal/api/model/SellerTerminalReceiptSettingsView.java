package com.tchalanet.server.core.sellerterminal.api.model;

public record SellerTerminalReceiptSettingsView(
    boolean autoPrint,
    int copyCount,
    boolean quickSale,
    String printerMode,
    String paperSize,
    String adapterPreference) {
  public static SellerTerminalReceiptSettingsView defaults() {
    return new SellerTerminalReceiptSettingsView(true, 1, true, "POS_DIRECT", "RECEIPT_58MM", null);
  }
}
