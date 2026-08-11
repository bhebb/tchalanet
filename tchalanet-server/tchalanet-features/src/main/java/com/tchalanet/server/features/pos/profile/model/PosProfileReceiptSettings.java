package com.tchalanet.server.features.pos.profile.model;

public record PosProfileReceiptSettings(
    boolean autoPrint,
    int copyCount,
    boolean quickSale,
    String printerMode,
    String paperSize,
    String adapterPreference) {
  public static PosProfileReceiptSettings defaults() {
    return new PosProfileReceiptSettings(true, 1, true, "POS_DIRECT", "RECEIPT_58MM", null);
  }
}
