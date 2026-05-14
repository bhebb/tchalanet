package com.tchalanet.server.platform.document.api.model;

public record DocumentOptions(Integer paperWidthMm, Integer qrSizePx) {
  public static DocumentOptions defaults() {
    return new DocumentOptions(null, null);
  }

  public int qrSizePxOrDefault(int fallback) {
    return qrSizePx == null || qrSizePx <= 0 ? fallback : qrSizePx;
  }
}
