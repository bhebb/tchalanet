package com.tchalanet.server.features.pos.profile.model;

public record PosProfileReceiptSettings(boolean autoPrint, int copyCount) {
  public static PosProfileReceiptSettings defaults() {
    return new PosProfileReceiptSettings(true, 1);
  }
}
