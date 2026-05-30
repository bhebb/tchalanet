package com.tchalanet.server.platform.document.api.model;

import com.tchalanet.server.platform.document.api.model.DocumentFormat;

/**
 * Internal profile used by platform.document and core.sales to drive generation.
 */
public record DocumentPrintProfile(
    DocumentFormat outputFormat,
    PaperSize paperSize,
    int textColumns,
    boolean monospace,
    boolean compact
) {
    public DocumentPrintProfile {
        if (outputFormat == null) {
            throw new IllegalArgumentException("outputFormat is required");
        }
        if (paperSize == null) {
            throw new IllegalArgumentException("paperSize is required");
        }
        if (textColumns <= 0) {
            throw new IllegalArgumentException("textColumns must be positive");
        }
        if (outputFormat == DocumentFormat.ESC_POS && paperSize == PaperSize.A4) {
            throw new IllegalArgumentException("ESC_POS does not support A4 paper");
        }
    }

    public static DocumentPrintProfile of(DocumentFormat outputFormat, PaperSize paperSize) {
        int columns;
        switch (paperSize) {
            case RECEIPT_58MM -> columns = 32;
            case RECEIPT_80MM -> columns = 42;
            default -> columns = 80;
        }

        boolean receiptPaper = paperSize == PaperSize.RECEIPT_58MM
            || paperSize == PaperSize.RECEIPT_80MM;

        return new DocumentPrintProfile(
            outputFormat,
            paperSize,
            columns,
            true,
            receiptPaper
        );
    }

    public boolean receiptPaper() {
        return paperSize == PaperSize.RECEIPT_58MM
            || paperSize == PaperSize.RECEIPT_80MM;
    }
}


