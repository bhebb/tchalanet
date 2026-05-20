package com.tchalanet.server.platform.document.api.model;

public record DocumentAsset(
    AssetKind kind,
    String name,
    byte[] bytes,
    String payload,
    Integer sizePx
) {

    public DocumentAsset {
        if (kind == null) {
            throw new IllegalArgumentException("asset kind is required");
        }

        if (bytes != null) {
            bytes = bytes.clone();
        }
    }

    @Override
    public byte[] bytes() {
        return bytes == null ? null : bytes.clone();
    }

    public static DocumentAsset qr(String payload, int sizePx) {
        return new DocumentAsset(AssetKind.QR, "qr", null, payload, sizePx);
    }

    public static DocumentAsset qrBytes(byte[] bytes, int sizePx) {
        return new DocumentAsset(AssetKind.QR, "qr", bytes, null, sizePx);
    }
}
