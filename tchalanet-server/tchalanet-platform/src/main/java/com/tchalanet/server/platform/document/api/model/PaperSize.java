package com.tchalanet.server.platform.document.api.model;

public enum PaperSize {
    RECEIPT_58MM(164.41f),
    RECEIPT_80MM(226.77f),
    A4(595.28f);

    private final float widthPoints;

    PaperSize(float widthPoints) {
        this.widthPoints = widthPoints;
    }

    public float widthPoints() {
        return widthPoints;
    }
}
