package com.tchalanet.server.platform.document.api.model;

public record DocumentAsset(
    String id, AssetKind kind, byte[] bytes, String payload, Integer sizePx) {

  public static DocumentAsset qr(String id, String payload, int sizePx) {
    return new DocumentAsset(id, AssetKind.QR, null, payload, sizePx);
  }

  public static DocumentAsset image(String id, byte[] bytes) {
    return new DocumentAsset(id, AssetKind.IMAGE, bytes, null, null);
  }
}
