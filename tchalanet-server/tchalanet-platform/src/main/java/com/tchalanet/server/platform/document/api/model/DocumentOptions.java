package com.tchalanet.server.platform.document.api.model;

import com.tchalanet.server.platform.document.internal.escpos.EscPosCodePage;

public record DocumentOptions(
    Integer qrSizePx,
    PaperSize paperSize,
    Boolean compact,
    EscPosCodePage escPosCodePage
) {

    public static DocumentOptions defaults() {
        return new DocumentOptions(280, PaperSize.RECEIPT_80MM, true, EscPosCodePage.CP850);
    }

    public static DocumentOptions receipt80mm() {
        return new DocumentOptions(280, PaperSize.RECEIPT_80MM, true, EscPosCodePage.CP850);
    }

    public static DocumentOptions receipt58mm() {
        return new DocumentOptions(240, PaperSize.RECEIPT_58MM, true, EscPosCodePage.CP850);
    }

    /**
     * A4 page layout: full-width PDF (595pt), larger QR, not compact.
     * Use for mobile print preview — renders as a readable A4 document,
     * not a narrow receipt strip.
     */
    public static DocumentOptions a4() {
        return new DocumentOptions(200, PaperSize.A4, false, null);
    }

    public int qrSizePxOrDefault(int fallback) {
        return qrSizePx != null && qrSizePx > 0 ? qrSizePx : fallback;
    }

    public PaperSize paperSizeOrDefault() {
        return paperSize == null ? PaperSize.RECEIPT_80MM : paperSize;
    }

    public EscPosCodePage escPosCodePageOrDefault() {
        return escPosCodePage == null ? EscPosCodePage.CP850 : escPosCodePage;
    }

    public boolean compactOrDefault() {
        return compact == null || compact;
    }
}
